package com.example.smartstorage.data.remote.llm

import com.example.smartstorage.domain.model.ParsedItem

/**
 * 解析结果统一清洗器：大模型结果与本地降级结果都经过这里，保证两条路径行为一致。
 *
 * 处理内容：trim 各字段；去掉名称首尾引号/标点；剥离名称尾部可能残留的连接词与语气成分
 * （如“…毛拖鞋都”“…挂钩均”这类来自“都在/均放在”句式的残留）；丢弃名称为空的结果。
 */
internal object ParsedItemSanitizer {

    /** 名称首尾需要去掉的引号与标点。 */
    private val EDGE_CHARS = setOf(
        '"', '\'', '“', '”', '「', '」', '‘', '’',
        '、', '，', ',', '。', '.', '；', ';', '：', ':', ' ',
    )

    /** 名称尾部可能残留的连接词 / 语气成分（按长到短匹配，避免“还有”先被“还”误伤）。 */
    private val TRAILING_TOKENS = listOf(
        "还有", "以及", "都", "均", "和", "与", "跟",
    )

    /** 清洗单条结果；名称为空返回 null（调用方据此丢弃该条）。 */
    fun sanitize(item: ParsedItem): ParsedItem? {
        val name = trimEdge(item.name)
        if (name.isEmpty()) return null
        return ParsedItem(
            name = name,
            location = trimEdge(item.location),
            description = item.description.trim(),
        )
    }

    /** 批量清洗；自动过滤名称为空的结果。 */
    fun sanitizeAll(items: List<ParsedItem>): List<ParsedItem> =
        items.mapNotNull { sanitize(it) }

    /** 去掉首尾引号/标点，并反复剥离名称尾部的连接/语气残留。 */
    private fun trimEdge(raw: String): String {
        var text = raw.trim()
        // 去首尾引号与标点
        while (text.isNotEmpty() && text.first() in EDGE_CHARS) text = text.drop(1)
        while (text.isNotEmpty() && text.last() in EDGE_CHARS) text = text.dropLast(1)
        text = text.trim()
        // 反复剥离尾部连接词/语气残留（可能叠加，如“…都”“…还有”）
        var changed = true
        while (changed && text.isNotEmpty()) {
            changed = false
            for (token in TRAILING_TOKENS) {
                // 仅当后面还留有名称主体时才剥离，避免把整个名称剥空
                if (text.length > token.length && text.endsWith(token)) {
                    text = text.dropLast(token.length).trimEnd()
                    changed = true
                    break
                }
            }
        }
        // 剥离后再去一次首尾标点，避免露出内部残留标点
        while (text.isNotEmpty() && text.first() in EDGE_CHARS) text = text.drop(1)
        while (text.isNotEmpty() && text.last() in EDGE_CHARS) text = text.dropLast(1)
        return text.trim()
    }
}