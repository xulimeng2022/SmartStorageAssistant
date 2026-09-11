package com.example.smartstorage.data.remote.vision

import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 视觉探测纯逻辑测试：响应判定 + 错误分类矩阵。 */
class VisionProbeEvaluatorTest {

    private val triLanguageJson =
        "{\"objects\":{\"zh-Hans\":[\"充电器\"],\"zh-Hant\":[\"充電器\"],\"en\":[\"charger\"]}," +
            "\"attributes\":{\"zh-Hans\":[\"黑色\"],\"zh-Hant\":[\"黑色\"],\"en\":[\"black\"]}," +
            "\"visibleText\":{\"zh-Hans\":[],\"zh-Hant\":[],\"en\":[]}," +
            "\"description\":{\"zh-Hans\":\"黑色充电器\",\"zh-Hant\":\"黑色充電器\",\"en\":\"black charger\"}," +
            "\"confidence\":0.9}"

    @Test
    fun fullStructuredResponseIsSupported() {
        assertEquals(VisionProbeResult.SUPPORTED, VisionProbeEvaluator.evaluate(triLanguageJson))
    }

    @Test
    fun fencedResponseWithSurroundingTextIsSupported() {
        val content = "这是结果：\n```json\n$triLanguageJson\n```\n以上。"
        assertEquals(VisionProbeResult.SUPPORTED, VisionProbeEvaluator.evaluate(content))
    }

    @Test
    fun englishVisualKeywordsWithoutJsonAreSupported() {
        val content = "The image shows a red circle and a blue square."
        assertEquals(VisionProbeResult.SUPPORTED, VisionProbeEvaluator.evaluate(content))
    }

    @Test
    fun chineseVisualKeywordsWithoutJsonAreSupported() {
        assertEquals(
            VisionProbeResult.SUPPORTED,
            VisionProbeEvaluator.evaluate("图片里有一个红色的圆和一个蓝色的方块。"),
        )
        assertEquals(
            VisionProbeResult.SUPPORTED,
            VisionProbeEvaluator.evaluate("圖片裡有一個紅色的圓和一個藍色的方塊。"),
        )
    }

    @Test
    fun blankResponseIsParseErrorNotUnsupported() {
        assertEquals(VisionProbeResult.RESPONSE_PARSE_ERROR, VisionProbeEvaluator.evaluate(""))
        assertEquals(VisionProbeResult.RESPONSE_PARSE_ERROR, VisionProbeEvaluator.evaluate("   \n "))
    }

    @Test
    fun unrelatedTextIsIncompatible() {
        assertEquals(VisionProbeResult.INCOMPATIBLE, VisionProbeEvaluator.evaluate("I can help with text only."))
    }

    @Test
    fun explicitVisionRejectionIsModelUnsupported() {
        val error = LlmRequestException(
            kind = LlmErrorKind.MODEL_OR_REQUEST,
            message = "bad request",
            httpCode = 400,
            detail = "This model does not support image input",
        )
        assertEquals(VisionProbeResult.MODEL_UNSUPPORTED, VisionProbeEvaluator.classify(error))
    }

    @Test
    fun genericUnsupportedParameterIsNotModelUnsupported() {
        val error = LlmRequestException(
            kind = LlmErrorKind.MODEL_OR_REQUEST,
            message = "bad request",
            httpCode = 400,
            detail = "unsupported parameter: temperature",
        )
        assertEquals(VisionProbeResult.BAD_REQUEST, VisionProbeEvaluator.classify(error))
        assertFalse(VisionProbeEvaluator.isExplicitVisionRejection("unsupported parameter: temperature"))
        assertFalse(VisionProbeEvaluator.isExplicitVisionRejection(null))
    }

    @Test
    fun httpErrorMatrix() {
        fun classify(kind: LlmErrorKind, code: Int? = null, detail: String? = null) =
            VisionProbeEvaluator.classify(
                LlmRequestException(kind = kind, message = "x", httpCode = code, detail = detail),
            )

        assertEquals(VisionProbeResult.AUTH_ERROR, classify(LlmErrorKind.AUTH, 401))
        assertEquals(VisionProbeResult.QUOTA_ERROR, classify(LlmErrorKind.QUOTA, 402))
        assertEquals(VisionProbeResult.RATE_LIMIT, classify(LlmErrorKind.RATE_LIMIT, 429))
        assertEquals(VisionProbeResult.SERVER_ERROR, classify(LlmErrorKind.SERVER, 503))
        assertEquals(VisionProbeResult.NETWORK_ERROR, classify(LlmErrorKind.NETWORK))
        assertEquals(VisionProbeResult.BAD_REQUEST, classify(LlmErrorKind.MISSING_CONFIG))
        assertEquals(VisionProbeResult.MODEL_NOT_FOUND, classify(LlmErrorKind.MODEL_OR_REQUEST, 404))
        assertEquals(
            VisionProbeResult.MODEL_NOT_FOUND,
            classify(LlmErrorKind.MODEL_OR_REQUEST, 400, "model not found: deepseek-v4.1-flash"),
        )
        assertEquals(VisionProbeResult.BAD_REQUEST, classify(LlmErrorKind.MODEL_OR_REQUEST, 400))
        assertEquals(VisionProbeResult.UNKNOWN_ERROR, classify(LlmErrorKind.UNKNOWN))
    }

    @Test
    fun failingResultsStayDistinct() {
        assertNotEquals(VisionProbeResult.AUTH_ERROR, VisionProbeResult.MODEL_UNSUPPORTED)
        assertTrue(VisionProbeEvaluator.isExplicitVisionRejection("当前模型不支持图片输入"))
        assertTrue(VisionProbeEvaluator.isExplicitVisionRejection("The model does not support vision input"))
    }
}
