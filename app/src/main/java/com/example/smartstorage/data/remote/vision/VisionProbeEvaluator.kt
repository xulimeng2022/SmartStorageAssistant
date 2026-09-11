package com.example.smartstorage.data.remote.vision

import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException
import java.util.Locale

/**
 * 视觉能力探测的纯逻辑判定：不依赖 Android，可在 JVM 单测中覆盖全部分支。
 *
 * 只有服务端明确声明当前模型/接口拒绝图片输入时，才判定 [VisionProbeResult.MODEL_UNSUPPORTED]；
 * 鉴权、额度、限流、网络、服务端、请求参数与解析问题分别归入对应错误，避免误报“不支持视觉”。
 */
object VisionProbeEvaluator {

    /** 合成探测图为白底 + 红圆 + 蓝方，响应只要提到其中一种颜色即认为模型确实读到了图片。 */
    private val EXPECTED_COLOR_KEYWORDS = listOf("red", "blue", "红", "紅", "蓝", "藍")

    /** 明确表示“不支持图片/视觉输入”的关键词（需同时命中视觉词与拒绝词）。 */
    private val VISION_TERMS = listOf(
        "image", "vision", "multimodal",
        "图片", "圖片", "图像", "圖像", "视觉", "視覺", "多模态", "多模態",
    )
    private val REJECTION_TERMS = listOf(
        "not support", "unsupported", "does not support",
        "不支持", "无法处理", "無法處理",
    )

    /** 解析 HTTP 200 的模型响应：结构 JSON 优先，其次校验是否包含合成图中的颜色。 */
    fun evaluate(content: String): VisionProbeResult {
        if (content.isBlank()) return VisionProbeResult.RESPONSE_PARSE_ERROR
        if (VisionJsonParser.parseAnalysis(content) != null) return VisionProbeResult.SUPPORTED
        val text = content.lowercase(Locale.ROOT)
        return if (EXPECTED_COLOR_KEYWORDS.any { text.contains(it) }) {
            VisionProbeResult.SUPPORTED
        } else {
            VisionProbeResult.INCOMPATIBLE
        }
    }

    /** 把类型化请求错误映射为探测结果。 */
    fun classify(error: LlmRequestException): VisionProbeResult = when (error.kind) {
        LlmErrorKind.MISSING_CONFIG -> VisionProbeResult.BAD_REQUEST
        LlmErrorKind.AUTH -> VisionProbeResult.AUTH_ERROR
        LlmErrorKind.QUOTA -> VisionProbeResult.QUOTA_ERROR
        LlmErrorKind.RATE_LIMIT -> VisionProbeResult.RATE_LIMIT
        LlmErrorKind.SERVER -> VisionProbeResult.SERVER_ERROR
        LlmErrorKind.NETWORK -> VisionProbeResult.NETWORK_ERROR
        LlmErrorKind.MODEL_OR_REQUEST -> when {
            isExplicitVisionRejection(error.detail) -> VisionProbeResult.MODEL_UNSUPPORTED
            isModelNotFound(error) -> VisionProbeResult.MODEL_NOT_FOUND
            else -> VisionProbeResult.BAD_REQUEST
        }
        LlmErrorKind.UNKNOWN -> VisionProbeResult.UNKNOWN_ERROR
    }

    /** 服务端响应是否明确拒绝图片输入（视觉词 + 拒绝词同时出现才算）。 */
    fun isExplicitVisionRejection(detail: String?): Boolean {
        val text = detail?.lowercase(Locale.ROOT).orEmpty()
        if (text.isBlank()) return false
        return VISION_TERMS.any { text.contains(it) } && REJECTION_TERMS.any { text.contains(it) }
    }

    /** 模型不存在/已下线（含 404 与常见“not found”描述）。 */
    private fun isModelNotFound(error: LlmRequestException): Boolean {
        if (error.httpCode == 404) return true
        val text = error.detail?.lowercase(Locale.ROOT).orEmpty()
        return text.contains("model not found") ||
            text.contains("does not exist") ||
            text.contains("已下线") ||
            text.contains("不存在")
    }
}
