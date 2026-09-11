package com.example.smartstorage.presentation.common

import com.example.smartstorage.R
import com.example.smartstorage.data.remote.vision.VisionProbeResult

/**
 * 视觉能力探测失败的显示层映射：每类失败给出可操作的三语文案资源。
 *
 * 只有服务端明确拒绝图片输入才提示“模型不支持”；鉴权、额度、限流、网络、超时、
 * 服务端、请求参数与解析问题分别提示各自的处理方式，避免误导用户更换模型。
 * [VisionProbeResult.SUPPORTED] 无失败文案，返回 null。
 */
fun VisionProbeResult.toFailureUiMessage(): UiMessage? = when (this) {
    VisionProbeResult.SUPPORTED -> null
    VisionProbeResult.MODEL_UNSUPPORTED -> UiMessage.Res(R.string.vision_error_model_unsupported)
    VisionProbeResult.AUTH_ERROR -> UiMessage.Res(R.string.vision_error_auth)
    VisionProbeResult.QUOTA_ERROR -> UiMessage.Res(R.string.vision_error_quota)
    VisionProbeResult.RATE_LIMIT -> UiMessage.Res(R.string.vision_error_rate_limit)
    VisionProbeResult.NETWORK_ERROR -> UiMessage.Res(R.string.vision_error_network)
    VisionProbeResult.TIMEOUT -> UiMessage.Res(R.string.vision_error_timeout)
    VisionProbeResult.SERVER_ERROR -> UiMessage.Res(R.string.vision_error_server)
    VisionProbeResult.BAD_REQUEST -> UiMessage.Res(R.string.vision_error_bad_request)
    VisionProbeResult.MODEL_NOT_FOUND -> UiMessage.Res(R.string.vision_error_model_not_found)
    VisionProbeResult.RESPONSE_PARSE_ERROR -> UiMessage.Res(R.string.vision_error_response_parse)
    VisionProbeResult.INCOMPATIBLE -> UiMessage.Res(R.string.vision_error_incompatible)
    VisionProbeResult.UNKNOWN_ERROR -> UiMessage.Res(R.string.vision_error_unknown)
}
