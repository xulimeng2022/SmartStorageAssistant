package com.example.smartstorage.presentation.common

import com.example.smartstorage.R
import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException

/** 把数据层的类型化请求错误映射为显示层资源消息（不在数据层拼接文案）。 */
fun LlmRequestException.toUiMessage(): UiMessage {
    val baseRes = when (kind) {
        LlmErrorKind.MISSING_CONFIG -> if (isFreeMode == true) {
            R.string.llm_error_missing_config_free
        } else {
            R.string.llm_error_missing_config_custom
        }
        LlmErrorKind.AUTH -> R.string.llm_error_auth
        LlmErrorKind.QUOTA -> R.string.llm_error_quota
        LlmErrorKind.RATE_LIMIT -> R.string.llm_error_rate_limit
        LlmErrorKind.MODEL_OR_REQUEST -> R.string.llm_error_model_or_request
        LlmErrorKind.SERVER -> R.string.llm_error_server
        LlmErrorKind.NETWORK -> R.string.llm_error_network
        LlmErrorKind.UNKNOWN -> R.string.llm_error_unknown
    }
    val detail = when {
        httpCode != null && detail != null -> UiMessage.Res(
            R.string.llm_error_with_http_detail,
            listOf(UiMessage.Res(baseRes), httpCode, detail),
        )
        httpCode != null -> UiMessage.Res(
            R.string.llm_error_with_http,
            listOf(UiMessage.Res(baseRes), httpCode),
        )
        detail != null -> UiMessage.Res(
            R.string.llm_error_with_detail,
            listOf(UiMessage.Res(baseRes), detail),
        )
        else -> UiMessage.Res(baseRes)
    }
    return UiMessage.Res(R.string.llm_error_prefix, listOf(detail))
}
