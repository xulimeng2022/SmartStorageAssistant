package com.example.smartstorage.data.remote.llm

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 大模型 HTTP 传输缝隙：负责真正发起一次 chat/completions 请求并返回 content。
 *
 * 抽成接口是为了在单元测试里用可控假实现做故障注入
 * （成功 / 超时 / 认证 / 限流 / 网络异常 / 响应格式错误），无需真实网络。
 */
interface LlmTransport {
    /**
     * 发起一次 chat/completions 请求：成功返回 content 文本；
     * 非 2xx 抛 [LlmRequestException]（按状态码分类脱敏）；网络异常（含超时）原样上抛。
     */
    fun postChatCompletion(
        baseUrl: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        userText: String,
        maxTokens: Int,
    ): String
}

/** OkHttp 实现：兼容用户填完整地址或仅填根地址两种方式，错误体摘要脱敏后分类。 */
class OkHttpLlmTransport(
    private val okHttpClient: OkHttpClient,
) : LlmTransport {

    override fun postChatCompletion(
        baseUrl: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        userText: String,
        maxTokens: Int,
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject()
            .put("model", modelName)
            .put("temperature", 0.0)
            .put("max_tokens", maxTokens)
            .put("messages", messages)
            .toString()

        val request = Request.Builder()
            .url(buildEndpoint(baseUrl))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // 读取错误体摘要（脱敏）后抛出类型化异常
                throw LlmErrorClassifier.exceptionFor(response.code, responseBody)
            }
            val root = JSONObject(responseBody)
            return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
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
}