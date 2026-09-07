package com.example.smartstorage.data.remote.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LlmErrorClassifier 的单元测试：HTTP 状态码分类、可重试判断与脱敏消息拼接。 */
class LlmErrorClassifierTest {

    @Test
    fun httpKindMapping() {
        assertNull(LlmErrorClassifier.kindForHttp(200))
        assertEquals(LlmErrorKind.AUTH, LlmErrorClassifier.kindForHttp(401))
        assertEquals(LlmErrorKind.AUTH, LlmErrorClassifier.kindForHttp(403))
        assertEquals(LlmErrorKind.QUOTA, LlmErrorClassifier.kindForHttp(402))
        assertEquals(LlmErrorKind.MODEL_OR_REQUEST, LlmErrorClassifier.kindForHttp(400))
        assertEquals(LlmErrorKind.MODEL_OR_REQUEST, LlmErrorClassifier.kindForHttp(404))
        assertEquals(LlmErrorKind.RATE_LIMIT, LlmErrorClassifier.kindForHttp(429))
        assertEquals(LlmErrorKind.SERVER, LlmErrorClassifier.kindForHttp(500))
        assertEquals(LlmErrorKind.UNKNOWN, LlmErrorClassifier.kindForHttp(0))
    }

    @Test
    fun retryableKinds() {
        assertTrue(LlmErrorClassifier.isRetryable(LlmErrorKind.RATE_LIMIT))
        assertTrue(LlmErrorClassifier.isRetryable(LlmErrorKind.SERVER))
        assertTrue(LlmErrorClassifier.isRetryable(LlmErrorKind.NETWORK))
        assertFalse(LlmErrorClassifier.isRetryable(LlmErrorKind.AUTH))
        assertFalse(LlmErrorClassifier.isRetryable(LlmErrorKind.QUOTA))
        assertFalse(LlmErrorClassifier.isRetryable(LlmErrorKind.MODEL_OR_REQUEST))
        assertFalse(LlmErrorClassifier.isRetryable(LlmErrorKind.MISSING_CONFIG))
        assertFalse(LlmErrorClassifier.isRetryable(LlmErrorKind.UNKNOWN))
    }

    @Test
    fun userMessageContainsReadableReasonPerKind() {
        assertEquals("API Key 无效或无权限，请到设置页检查密钥或切换到自定义模式（HTTP 401）",
            LlmErrorClassifier.userMessage(LlmErrorKind.AUTH, 401, null))
        assertTrue(LlmErrorClassifier.userMessage(LlmErrorKind.MISSING_CONFIG, null, null).contains("SILICONFLOW_API_KEY"))
        assertTrue(LlmErrorClassifier.userMessage(LlmErrorKind.RATE_LIMIT, 429, null).contains("请求过于频繁"))
        assertTrue(LlmErrorClassifier.userMessage(LlmErrorKind.SERVER, 503, null).contains("服务暂时不可用"))
        assertTrue(LlmErrorClassifier.userMessage(LlmErrorKind.NETWORK, null, null).contains("网络连接失败"))
    }

    @Test
    fun exceptionForCarriesKindAndBodySummary() {
        val body = """{"error":{"message":"当前模型 Qwen/Qwen2.5-7B-Instruct 已下线，请更换模型"}}"""
        val ex = LlmErrorClassifier.exceptionFor(404, body)
        assertEquals(LlmErrorKind.MODEL_OR_REQUEST, ex.kind)
        assertTrue(ex.message!!.contains("HTTP 404"))
        // 摘要来自响应体 error.message，且不含请求本身
        assertTrue(ex.message!!.contains("已下线"))
    }

    @Test
    fun summarizeBodyTruncatesAndSingleLines() {
        val long = "msg" + "长".repeat(200)
        val json = """{"message":"$long"}"""
        val summary = LlmErrorClassifier.summarizeBody(json)!!
        assertTrue(summary.length <= 121)
        assertFalse(summary.contains("\n"))
        assertNull(LlmErrorClassifier.summarizeBody(null))
        assertNull(LlmErrorClassifier.summarizeBody(""))
        assertNull(LlmErrorClassifier.summarizeBody("""{"code":123}"""))
    }
}