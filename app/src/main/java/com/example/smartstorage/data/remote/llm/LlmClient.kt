package com.example.smartstorage.data.remote.llm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/** 免费模式请求超时（UI 据此弹出“解析超时”引导对话框）。 */
class LlmTimeoutException : Exception("免费模型请求超时")

/** 大模型解析出的物品字段。 */
data class ParsedItem(
    val name: String,
    val location: String,
    val description: String,
)

/**
 * 大模型客户端：调用任意 OpenAI 兼容接口（DeepSeek / 通义千问 / OpenAI 等），
 * 把口语描述解析为结构化的物品字段（物品名/地点/备注）。
 *
 * 常驻开启：只要配置有效（baseUrl / modelName / apiKey 齐全）就发起请求；
 * 配置缺失或调用失败时降级（返回 null）并用 Toast 提示用户检查配置。
 */
@Singleton
class LlmClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 解析一段口语描述。
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
        chatCompletion(systemPrompt, rawText)?.let { parseJsonContent(it) }
    }

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
     * 发起一次 chat/completions 请求，返回模型输出的 content 文本。
     *
     * 配置不完整或调用失败时弹出 Toast 并返回 null。
     */
    private suspend fun chatCompletion(systemPrompt: String, userText: String): String? {
        val config = settingsRepository.getConfig()
        // 按模式选择实际请求参数：免费模式用内置固定配置（不写 DataStore）；自定义模式用存储配置
        val isFreeMode = config.mode == AiConfig.MODE_FREE
        val baseUrl = if (isFreeMode) FreeModel.BASE_URL else config.baseUrl
        val modelName = if (isFreeMode) FreeModel.MODEL else config.modelName
        val apiKey = if (isFreeMode) FreeModel.API_KEY else config.apiKey
        // 配置不完整：降级 + Toast 提示
        if (baseUrl.isBlank() || modelName.isBlank() || apiKey.isBlank()) {
            showToast(
                if (isFreeMode) {
                    "免费服务暂时不可用，请切换到自定义模式配置自己的 API Key"
                } else {
                    "AI 解析配置不完整，请到设置页填写 Base URL、模型版本和 API Key"
                }
            )
            return null
        }
        return try {
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userText))

            val body = JSONObject()
                .put("model", modelName)
                .put("temperature", 0.0)
                .put("max_tokens", 256)
                .put("messages", messages)
                .toString()

            val request = Request.Builder()
                .url(buildEndpoint(baseUrl))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code}")
                }
                val root = JSONObject(response.body?.string().orEmpty())
                root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: SocketTimeoutException) {
            // 免费模式超时：抛专用异常，由 UI 弹“解析超时”引导；自定义模式保持普通提示
            if (isFreeMode) throw LlmTimeoutException()
            showToast("AI 解析超时，请稍后重试或检查网络")
            null
        } catch (e: Exception) {
            // 调用失败：降级 + Toast 提示检查配置
            showToast("AI 解析失败：${e.message ?: "未知错误"}，请检查设置中的模型名称与 API 配置")
            null
        }
    }



    /** 拼接 chat/completions 端点：兼容用户填完整地址或仅填根地址两种方式。 */
    private fun buildEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed
        } else {
            "$trimmed/chat/completions"
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
}