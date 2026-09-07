package com.example.smartstorage.data.remote.llm

import com.example.smartstorage.domain.model.ParsedItem

/**
 * 本地降级解析器：大模型不可用 / 解析失败 / 返回空时，用确定性的规则尽力拆出多条物品。
 *
 * 规则概述（保守优先，无法可靠识别的输入保留原文，避免误拆品牌与成套物品）：
 * 1. 按句读（，。；！？换行等）切成子句；每句独立解析。
 * 2. 在一个子句里取“最后一个位置动词”（放在/放到/放于/存于/放进/放入/收在/摆在/搁在，
 *    否则退化为单字“在”）把句子切成“物品部分”与“地点”；
 *    动词前若紧跟“都/均/全/全都/全部”等共享地点标记，则一并消耗，不再混入名称。
 * 3. 对“物品部分”做枚举拆分：顿号“、”直接拆分；连接词“和/跟/与/以及/还有”仅在
 *    出现共享地点标记、或该句没有地点、或名称里带顿号时才拆分，避免误拆“强生和强生牌…”
 *    这类品牌复读名称与成套物品。
 * 4. 数量词（一堆/一些/一套…）只是名称的一部分，绝不据此虚构数量或多条记录。
 * 5. 拆分后每条物品共享该句地点；无法拆分时整句作为一条（名称=原文、地点留空）。
 */
internal object LocalDescriptionParser {

    /** 多字位置动词：优先匹配；命中后动词本身不算进地点文本。 */
    private val MULTI_CHAR_VERBS = listOf(
        "放置在", "放在", "放到", "放于", "存于", "放进", "放入", "收在", "摆在", "搁在",
    )

    /** 共享地点标记：动词前紧跟这些词时表示多个物品共用一个地点。 */
    private val SHARED_MARKERS = listOf("全都", "全部", "都", "均", "全")

    /** 句读分隔符：逗号、句号、分号、感叹/问号与换行（顿号“、”保留用于枚举识别）。 */
    private val CLAUSE_SPLIT_REGEX = Regex("[，,。．；;！？!?\\n]+")

    /** 连接词（枚举拆分用）。 */
    private val CONNECTOR_REGEX = Regex("和|跟|与|以及|还有")

    /** 子句开头的顺承连接词 / 宾语提前标记（“还有/以及/请把/把…”），切句后剥离。 */
    private val LEADING_CONNECTORS = listOf("请把", "请将", "还有", "以及", "然后", "接着", "另外", "把", "将", "请")

    /** 品牌复读后缀：右侧以“左侧文本 + 后缀”开头时视为品牌/固定名称，不拆分。 */
    private val BRAND_SUFFIXES = listOf("牌子", "牌", "氏", "家", "的")

    /** 解析整段口语描述，返回 0~N 条（结果建议再经 [ParsedItemSanitizer] 清洗）。 */
    fun parse(rawText: String): List<ParsedItem> {
        val text = rawText.trim()
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<ParsedItem>()
        text.split(CLAUSE_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { clause -> parseClause(stripLeadingConnectors(clause), result) }
        return result
    }

    /** 解析单个子句并把结果追加到 [out]。 */
    private fun parseClause(clause: String, out: MutableList<ParsedItem>) {
        if (clause.isEmpty()) return
        val split = findLocationSplit(clause)
        if (split == null) {
            // 全句没有地点：当作“物品部分”，仍可做枚举拆分（如“雨衣和拖鞋”）
            appendItems(clause, location = "", allowConnectorSplit = true, out = out)
            return
        }
        val (verbStart, verbLength) = split
        // 消耗动词前紧跟的共享地点标记（都/均/全…），避免“都”残留在名称里
        val nameEnd = consumeSharedMarkers(clause, verbStart)
        val namePart = clause.substring(0, nameEnd).trim()
        val location = clause.substring(verbStart + verbLength).trim()
        if (namePart.isEmpty()) {
            // 退化输入（如“放在柜子里的雨衣”）：整句保留为一条，避免丢原文
            out += ParsedItem(name = clause, location = "", description = "")
            return
        }
        // 出现共享标记、或该句本就没有地点时，允许按“和/跟/与”继续拆分枚举
        val allowConnectorSplit = nameEnd < verbStart || location.isEmpty()
        appendItems(namePart, location, allowConnectorSplit, out)
    }

    /**
     * 找到“物品部分 / 地点”的切分点；返回动词起点与长度；找不到返回 null。
     *
     * 多字动词按“结束位置最靠后（并列时起点更靠前）”选取，避免“放置在”里的“放在”
     * 被重复匹配后切进动词内部。
     */
    private fun findLocationSplit(clause: String): Pair<Int, Int>? {
        var bestStart = -1
        var bestEnd = -1
        for (verb in MULTI_CHAR_VERBS) {
            var from = 0
            while (true) {
                val idx = clause.indexOf(verb, from)
                if (idx < 0) break
                val end = idx + verb.length
                if (end > bestEnd || (end == bestEnd && idx < bestStart)) {
                    bestStart = idx
                    bestEnd = end
                }
                from = end
            }
        }
        if (bestStart >= 0) return bestStart to (bestEnd - bestStart)
        // 没有多字动词时，退化为最后一个“在”
        val lastIn = clause.lastIndexOf("在")
        return if (lastIn >= 0) lastIn to 1 else null
    }

    /** 从 [verbStart] 往前消耗共享地点标记（含中间空白），返回“物品部分”的结束下标。 */
    private fun consumeSharedMarkers(clause: String, verbStart: Int): Int {
        var end = verbStart
        var changed = true
        while (changed) {
            changed = false
            // 允许标记与动词之间存在空白
            while (end > 0 && clause[end - 1].isWhitespace()) {
                end--
                changed = true
            }
            for (marker in SHARED_MARKERS) {
                if (end >= marker.length &&
                    clause.substring(end - marker.length, end) == marker
                ) {
                    end -= marker.length
                    changed = true
                    break
                }
            }
        }
        return end
    }

    /** 把“物品部分”按枚举规则拆成多条并追加；无法拆分时整段作为一条。 */
    private fun appendItems(
        namePart: String,
        location: String,
        allowConnectorSplit: Boolean,
        out: MutableList<ParsedItem>,
    ) {
        val names = if (allowConnectorSplit || namePart.contains('、')) {
            splitEnumeration(namePart)
        } else {
            listOf(namePart)
        }
        names.forEach { name ->
            val trimmed = name.trim()
            if (trimmed.isNotEmpty()) {
                out += ParsedItem(name = trimmed, location = location, description = "")
            }
        }
    }

    /** 先按顿号拆分，再对每个片段按连接词拆分。 */
    private fun splitEnumeration(namePart: String): List<String> {
        val result = mutableListOf<String>()
        namePart.split('、')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { token -> result += splitTokenByConnector(token) }
        return result
    }

    /** 按连接词拆分单个片段；存在品牌复读或空/过短边界时整体保留，避免误拆。 */
    private fun splitTokenByConnector(token: String): List<String> {
        val matches = CONNECTOR_REGEX.findAll(token).toList()
        if (matches.isEmpty()) return listOf(token)
        var prev = 0
        for (m in matches) {
            val left = token.substring(prev, m.range.first).trim()
            val right = token.substring(m.range.last + 1).trim()
            if (left.isEmpty() || right.isEmpty() ||
                left.length < 2 || right.length < 2 ||
                isBrandRepetition(left, right)
            ) {
                // 任一边界不安全：整段保留，交给用户确认
                return listOf(token)
            }
            prev = m.range.last + 1
        }
        return token.split(CONNECTOR_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** 右侧以“左侧文本 + 品牌后缀”开头时，视为品牌/固定名称复读（如“强生和强生牌的创可贴”）。 */
    private fun isBrandRepetition(left: String, right: String): Boolean {
        if (right.startsWith(left)) return true
        return BRAND_SUFFIXES.any { right.startsWith(left + it) }
    }

    /** 去掉子句开头的顺承连接词 / 宾语提前标记。 */
    private fun stripLeadingConnectors(clause: String): String {
        var text = clause.trimStart()
        var changed = true
        while (changed && text.isNotEmpty()) {
            changed = false
            for (connector in LEADING_CONNECTORS) {
                if (text.startsWith(connector)) {
                    text = text.removePrefix(connector).trimStart()
                    changed = true
                    break
                }
            }
        }
        return text
    }
}