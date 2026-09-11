package com.example.smartstorage.data.remote.vision

import com.example.smartstorage.data.remote.llm.LlmErrorClassifier
import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** OpenAI 兼容多模态传输缝隙。 */
interface VisionTransport {
    fun postVisionCompletion(
        baseUrl: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray,
        mimeType: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): String

    companion object {
        const val DEFAULT_MAX_TOKENS = 1000
    }
}

class OkHttpVisionTransport(
    private val okHttpClient: OkHttpClient,
) : VisionTransport {
    override fun postVisionCompletion(
        baseUrl: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray,
        mimeType: String,
        maxTokens: Int,
    ): String {
        val encoded = Base64.getEncoder().encodeToString(imageBytes)
        val imageUrl = "data:" + mimeType + ";base64," + encoded
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", userText))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", imageUrl)),
            )
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", content))
        val body = JSONObject()
            .put("model", modelName)
            .put("temperature", 0.0)
            .put("max_tokens", maxTokens)
            .put("messages", messages)
            .toString()
        val request = Request.Builder()
            .url(buildEndpoint(baseUrl))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw LlmErrorClassifier.exceptionFor(response.code, responseBody)
            }
            val message = runCatching {
                JSONObject(responseBody)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
            }.getOrElse {
                throw LlmRequestException(
                    kind = LlmErrorKind.UNKNOWN,
                    message = "vision response shape is not compatible",
                    httpCode = response.code,
                )
            }
            // 兼容推理模型：content 为空时回退 reasoning_content，仍由上层按协议解析。
            return message.optString("content").ifBlank { message.optString("reasoning_content") }
        }
    }

    private fun buildEndpoint(baseUrl: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return if (trimmed.endsWith("/chat/completions")) trimmed else trimmed + "/chat/completions"
    }
}
