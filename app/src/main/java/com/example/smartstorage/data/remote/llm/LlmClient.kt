package com.example.smartstorage.data.remote.llm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.SettingsRepository
import com.example.smartstorage.domain.model.ParsedItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/** 免费模式请求超时（UI 据此弹出“解析超时”引导对话框）。 */
class LlmTimeoutException : Exception("免费模型请求超时")

/** 批量解析的结果来源，用于告知用户当前结果是模型识别还是本地规则兜底。 */
enum class ParseSource {
    /** 模型成功返回结构化结果。 */
    MODEL,

    /** 模型失败/不可用/返回为空，已回退本地规则解析。 */
    LOCAL_FALLBACK,
}

/** 批量解析结果：物品列表 + 来源 + 失败/降级原因（warning 非空表示模型路径未走通）。 */
data class ParseItemsResult(
    val items: List<ParsedItem>,
    val source: ParseSource,
    val warning: String?,
)

/** 单次 chat/completions 调用的结果（content 与 error 二选一）。 */
private data class LlmCallResult(
    val content: String?,
    val error: LlmRequestException?,
)

/**
 * 大模型客户端：调用任意 OpenAI 兼容接口（DeepSeek / 通义千问 / OpenAI 等），
 * 把口语描述解析为结构化的物品字段（物品名/地点/备注）。
 *
 * 常驻开启：只要配置有效（baseUrl / modelName / apiKey 齐全）就发起请求；
 * 调用失败按 [LlmErrorClassifier] 分类（认证/额度/限流/模型/服务端/网络/配置缺失），
 * 对可重试错误做有界重试；免费模式超时抛 [LlmTimeoutException] 由 UI 弹“解析超时”引导。
 * 批量解析 [parseItems] 在模型不可用时回退本地规则，并通过 [ParseItemsResult.warning]
 * 明确告知用户当前识别方式，避免“静默降级、假装解析完成”。
 */
@Singleton
class LlmClient @Inject constructor(
    private val transport: LlmTransport,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        /** 批量解析使用更大的输出上限，避免多物品 JSON 数组被截断。 */
        private const val BATCH_MAX_TOKENS = 1200

        /** 可重试错误之间的退避时长（毫秒）。 */
        private const val RETRY_BACKOFF_MILLIS = 300L

        /** 日志标签（只记录阶段/耗时/错误类型，不记录 Key 与用户原文）。 */
        private const val TAG = "LlmClient"
    }

    /**
     * 解析一段口语描述（单条场景）。
     *
     * @return 结构化物品字段；配置缺失或调用失败时返回 null（调用方降级处理）。
     */
    suspend fun parseItem(rawText: String): ParsedItem? = withContext(Dispatchers.IO) {
        val systemPrompt = """
            你是一个物品收纳专家。请从用户输入的文本中提取物品名称（name）、存放地点（location）和描述（description）。
            要求：
            - 只返回纯 JSON 对象，格式为 {"name":"", "location":"", "description":""}
            - 不要包含任何其他文字、解释、Markdown 格式或代码块标记
            - 如果某个字段无法提取，设为空字符串
            - 必须返回完整的 JSON 对象，以 { 开头，以 } 结尾
        """.trimIndent()
        val parsed = chatCompletion(systemPrompt, rawText)?.let { parseJsonContent(it) }
        ParsedItemSanitizer.sanitize(parsed ?: return@withContext null)
    }

    /**
     * 批量解析一段口语描述（可能包含多条物品）。
     *
     * 提示词要求返回严格 JSON 数组，并覆盖共享地点 / 不同地点 / 品牌成套名称 / 数量词等场景；
     * 响应经 [ParsedItemSanitizer] 统一清洗（保证与本地降级行为一致）。
     * 模型失败（含配置缺失、认证、限流、服务端、网络等非超时错误）时回退
     * [LocalDescriptionParser] 本地拆分，并把失败原因放入 [ParseItemsResult.warning]，
     * 由 UI 明确提示“当前为本地规则结果”，绝不静默假装模型解析完成。
     * 免费模式超时会抛出 [LlmTimeoutException]，由调用方（UI）弹出“解析超时”引导。
     */
    suspend fun parseItems(rawText: String): ParseItemsResult = withContext(Dispatchers.IO) {
        var source = ParseSource.MODEL
        var warning: String? = null
        val modelItems: List<ParsedItem> = try {
            val call = chatCompletionCore(buildBatchSystemPrompt(), rawText, maxTokens = BATCH_MAX_TOKENS)
            if (call.error != null) {
                // 模型调用失败：回退本地并记录原因
                source = ParseSource.LOCAL_FALLBACK
                warning = call.error.message
                emptyList()
            } else {
                val sanitized = ParsedItemSanitizer.sanitizeAll(
                    call.content?.let { parseJsonArray(it) } ?: emptyList(),
                )
                if (sanitized.isEmpty()) {
                    // 模型返回成功但没解析出有效字段：同样回退本地并说明
                    source = ParseSource.LOCAL_FALLBACK
                    warning = "模型未返回有效结果"
                    emptyList()
                } else {
                    sanitized
                }
            }
        } catch (e: LlmTimeoutException) {
            // 超时直接上抛，UI 弹“解析超时”对话框，不做静默降级
            throw e
        } catch (e: LlmRequestException) {
            source = ParseSource.LOCAL_FALLBACK
            warning = e.message
            emptyList()
        } catch (e: Exception) {
            source = ParseSource.LOCAL_FALLBACK
            warning = e.message ?: "未知错误"
            emptyList()
        }
        if (modelItems.isNotEmpty()) {
            ParseItemsResult(items = modelItems, source = ParseSource.MODEL, warning = null)
        } else {
            // 本地兜底解析：与模型结果走同一套清洗逻辑，保证两条路径行为一致
            val localItems = ParsedItemSanitizer.sanitizeAll(LocalDescriptionParser.parse(rawText))
            ParseItemsResult(items = localItems, source = source, warning = warning)
        }
    }

    /**
     * 纯本地降级解析：不发起任何网络请求，直接用规则把整段文本拆成物品草稿。
     *
     * 用于免费模式超时后用户选择「本地识别继续」等场景；结果来源固定为
     * [ParseSource.LOCAL_FALLBACK]，是否以及如何提示由调用方决定。
     */
    suspend fun parseItemsLocal(rawText: String): ParseItemsResult = withContext(Dispatchers.IO) {
        ParseItemsResult(
            items = ParsedItemSanitizer.sanitizeAll(LocalDescriptionParser.parse(rawText)),
            source = ParseSource.LOCAL_FALLBACK,
            warning = null,
        )
    }

    /** 批量解析的系统提示词：少样本覆盖共享地点、不同地点、品牌/成套、数量词与缺地点场景。 */
    private fun buildBatchSystemPrompt(): String = """
        你是一个物品收纳解析助手。用户会输入一条或多条物品口语描述，请按以下规则处理：
        1. 识别输入中包含的所有独立物品；每个物品输出一个 JSON 对象，所有结果放在一个 JSON 数组中，
           严格输出格式：[{"name":"物品名","location":"存放地点","description":"备注"}]。
           只返回 JSON 数组本身，不要包含任何解释、Markdown 标记或代码块；无法可靠解析时返回 []。
        2. “name”为必填且只含物品本身与有效修饰（如“学校发的”“自己的”“红色的”）；字段提取不到时填空字符串。
        3. 多个独立物品要拆成多条记录：
           - 共享地点：“学校发的雨衣和自己的毛拖鞋都在左边最下面开门柜子里”→ 两条，name 分别为“学校发的雨衣”“自己的毛拖鞋”，location 相同；
           - “夹子和墙壁挂钩都在左下抽屉里”→ 两条，name 分别为“夹子”“墙壁挂钩”，location 相同；
           - “雨衣、拖鞋和夹子都在柜子里”→ 三条，location 相同。
        4. 不同物品有不同地点时分别匹配，例如“红色的笔在柜子里，蓝色的笔在抽屉里”→ 两条，各自对应地点。
        5. “都”“均”“还有”“和”“在”等连接词、方位与语气成分不得拼进 name 或 location。
        6. 品牌、固定名称与成套物品不要按“和”拆分：“强生和强生牌的创可贴在抽屉里”“一套桌椅在阳台”都只输出一条。
        7. 不精确数量词只输出一条，不要把“一堆”等当作数量虚构多条或虚构确切数量；
           “一堆消毒液试用装在左下抽屉里”→ 一条，数量词“一堆”保留在 name 或 description 中。
        8. 用户没有说地点时 location 设为空字符串（如“雨衣和拖鞋”→ 两条，location 均为空），不要编造地点。
    """.trimIndent()

    /**
     * 解析一段自然语言，提取物品搜索关键词（用于首页“智能解析”按钮）。
     *
     * @return 提取到的关键词（去掉可能的首尾引号）；配置缺失或调用失败时返回 null（调用方降级用原文搜索）。
     */
    suspend fun parseSearchKeyword(rawText: String): String? = withContext(Dispatchers.IO) {
        val systemPrompt = """
            你是一个物品搜索助手。用户想找到他之前存放的某件物品，请从用户的自然语言描述中提取出最关键的物品搜索关键词，
            只返回关键词，不要返回其他内容。例如：用户说“我之前把红色充电器放哪了”，返回“红色充电器”。
            用户说“帮我找一下那个绿色的杯子”，返回“绿色杯子”。如果无法提取，返回用户输入的原文。
        """.trimIndent()
        val keyword = chatCompletion(systemPrompt, rawText)?.trim()
        keyword?.takeIf { it.isNotEmpty() }?.let { stripQuotes(it) }
    }

    /** 去掉模型输出中可能带的首尾引号。 */
    private fun stripQuotes(text: String): String = text
        .removePrefix("\"")
        .removeSuffix("\"")
        .removePrefix("'")
        .removeSuffix("'")
        .trim()

    /**
     * 对外便捷入口（单条解析 / 搜索用）：调用 [chatCompletionCore]，
     * 失败时 Toast 提示并返回 null；免费模式超时抛 [LlmTimeoutException]。
     */
    private suspend fun chatCompletion(
        systemPrompt: String,
        userText: String,
        maxTokens: Int = 256,
    ): String? {
        val result = chatCompletionCore(systemPrompt, userText, maxTokens)
        if (result.error != null) {
            showToast("AI 解析失败：${result.error.message}")
        }
        return result.content
    }

    /**
     * 核心请求：读取配置、发起 chat/completions、分类错误并对可重试错误做有界重试。
     *
     * @return content 或类型化 error；免费模式超时抛 [LlmTimeoutException]（不做本地降级）。
     */
    private suspend fun chatCompletionCore(
        systemPrompt: String,
        userText: String,
        maxTokens: Int = 256,
    ): LlmCallResult {
        val config = settingsRepository.getConfig()
        // 按模式选择实际请求参数：免费模式用内置固定配置（不写 DataStore）；自定义模式用存储配置
        val isFreeMode = config.mode == AiConfig.MODE_FREE
        val baseUrl = if (isFreeMode) FreeModel.BASE_URL else config.baseUrl
        val modelName = if (isFreeMode) FreeModel.MODEL else config.modelName
        val apiKey = if (isFreeMode) FreeModel.API_KEY else config.apiKey
        // 配置不完整：返回“配置缺失”类型错误，由调用方决定提示方式
        if (baseUrl.isBlank() || modelName.isBlank() || apiKey.isBlank()) {
            val message = if (isFreeMode) {
                "当前安装包未内置免费模型 Key（构建时需配置 SILICONFLOW_API_KEY），请到设置页切换到自定义模式填写自己的 API Key"
            } else {
                "AI 解析配置不完整，请到设置页填写 Base URL、模型版本和 API Key"
            }
            return LlmCallResult(null, LlmRequestException(LlmErrorKind.MISSING_CONFIG, message))
        }

        var attempt = 0
        while (true) {
            attempt++
            val startedAt = System.currentTimeMillis()
            // 每次尝试返回“是否可重试 + 结果”；成功直接返回
            val attemptResult: Pair<Boolean, LlmCallResult> = try {
                val content = transport.postChatCompletion(
                    baseUrl, modelName, apiKey, systemPrompt, userText, maxTokens,
                )
                Log.i(
                    TAG,
                    "LLM 请求成功 mode=${if (isFreeMode) "free" else "custom"} attempt=$attempt " +
                        "耗时=${System.currentTimeMillis() - startedAt}ms",
                )
                return LlmCallResult(content, null)
            } catch (e: SocketTimeoutException) {
                Log.w(
                    TAG,
                    "LLM 请求超时 mode=${if (isFreeMode) "free" else "custom"} attempt=$attempt " +
                        "耗时=${System.currentTimeMillis() - startedAt}ms",
                )
                // 免费模式超时：抛专用异常，由 UI 弹“解析超时”引导（可降级到本地规则）；
                // 自定义模式提示稍后重试；超时不自动重试，避免重复计费与长时间等待
                if (isFreeMode) throw LlmTimeoutException()
                false to LlmCallResult(
                    null,
                    LlmRequestException(LlmErrorKind.NETWORK, "AI 解析超时，请稍后重试或检查网络"),
                )
            } catch (e: LlmRequestException) {
                Log.w(TAG, "LLM 请求失败 kind=${e.kind} attempt=$attempt")
                // 已分类错误：仅对可重试种类（限流/服务端/网络）做有界重试
                val canRetry = attempt < LlmErrorClassifier.MAX_ATTEMPTS && LlmErrorClassifier.isRetryable(e.kind)
                canRetry to LlmCallResult(null, e)
            } catch (e: IOException) {
                Log.w(TAG, "LLM 网络异常 attempt=$attempt: ${e.javaClass.simpleName}")
                // 网络类错误：可重试
                val canRetry = attempt < LlmErrorClassifier.MAX_ATTEMPTS
                canRetry to LlmCallResult(
                    null,
                    LlmRequestException(LlmErrorKind.NETWORK, "网络连接失败，请检查网络后重试"),
                )
            } catch (e: Exception) {
                Log.w(TAG, "LLM 未知异常 attempt=$attempt: ${e.javaClass.simpleName}")
                // 其它（如响应体 JSON 解析失败）：不可重试，避免重复请求
                false to LlmCallResult(null, LlmRequestException(LlmErrorKind.UNKNOWN, e.message ?: "未知错误"))
            }
            if (!attemptResult.first) return attemptResult.second
            delay(RETRY_BACKOFF_MILLIS)
        }
    }

    /** 主线程弹出 Toast。 */
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 从模型输出中提取 JSON 并解析（含容错处理）。
     *
     * 预处理：去除 Markdown 代码块标记；若以 {"name": 开头、以 " 结尾但缺少 }，补全 }；
     * JSON 解析失败时用正则直接提取字段；全部失败则打印完整返回并返回 null（上层降级）。
     */
    private fun parseJsonContent(content: String): ParsedItem? {
        var cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 容错：缺少结尾 } 时补全
        if (cleaned.startsWith("{\"name\":") && cleaned.endsWith("\"") && !cleaned.endsWith("}")) {
            cleaned = "$cleaned}"
        }

        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        val jsonText = if (start >= 0 && end > start) {
            cleaned.substring(start, end + 1)
        } else {
            cleaned
        }

        return try {
            val json = JSONObject(jsonText)
            ParsedItem(
                name = json.optString("name", "").trim(),
                location = json.optString("location", "").trim(),
                description = json.optString("description", "").trim(),
            )
        } catch (e: Exception) {
            // 正则直接提取字段
            val name = extractField(jsonText, "name")
            val location = extractField(jsonText, "location")
            val description = extractField(jsonText, "description")
            if (name == null && location == null && description == null) {
                // 全部失败：打印完整返回便于调试，返回 null 由上层降级
                Log.e("LlmClient", "JSON 解析失败，原始返回：$content", e)
                null
            } else {
                ParsedItem(name ?: "", location ?: "", description ?: "")
            }
        }
    }

    /** 用正则从 JSON 文本中提取指定字段值（未找到返回 null）。 */
    private fun extractField(json: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            .find(json)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * 容错解析模型返回的 JSON 数组：支持数组、单个对象、Markdown 代码块包裹等情况。
     * 全部失败返回空列表（由上层降级）；字段清洗由上层统一执行。
     */
    private fun parseJsonArray(content: String): List<ParsedItem> {
        var cleaned = content
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val arrStart = cleaned.indexOf('[')
        val arrEnd = cleaned.lastIndexOf(']')
        // 非数组（单对象或异常文本）：尝试按单条解析
        if (arrStart < 0 || arrEnd <= arrStart) {
            return parseJsonContent(cleaned)?.let { listOf(it) } ?: emptyList()
        }
        return try {
            val arr = JSONArray(cleaned.substring(arrStart, arrEnd + 1))
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                ParsedItem(
                    name = obj.optString("name", "").trim(),
                    location = obj.optString("location", "").trim(),
                    description = obj.optString("description", "").trim(),
                )
            }
        } catch (e: Exception) {
            Log.e("LlmClient", "批量 JSON 解析失败，原始返回：$content", e)
            emptyList()
        }
    }
}
