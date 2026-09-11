package com.example.smartstorage.presentation

import com.example.smartstorage.R
import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException
import com.example.smartstorage.data.remote.vision.VisionProbeResult
import com.example.smartstorage.presentation.common.UiMessage
import com.example.smartstorage.presentation.common.toFailureUiMessage
import com.example.smartstorage.presentation.common.toUiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 临时 UI 消息映射测试：数据层只给类型，资源 ID 与参数在显示层确定。 */
class UiMessageMappingTest {

    @Test
    fun authErrorWithHttpCodeMapsToNestedResourceMessage() {
        val message = LlmRequestException(
            kind = LlmErrorKind.AUTH,
            message = "x",
            httpCode = 401,
        ).toUiMessage()

        val prefix = message as UiMessage.Res
        assertEquals(R.string.llm_error_prefix, prefix.id)
        val detail = prefix.args.single() as UiMessage.Res
        assertEquals(R.string.llm_error_with_http, detail.id)
        val base = detail.args[0] as UiMessage.Res
        assertEquals(R.string.llm_error_auth, base.id)
        assertEquals(401, detail.args[1])
    }

    @Test
    fun missingConfigDistinguishesFreeAndCustomMode() {
        val free = (LlmRequestException(LlmErrorKind.MISSING_CONFIG, "x", isFreeMode = true)
            .toUiMessage() as UiMessage.Res).id
        val custom = (LlmRequestException(LlmErrorKind.MISSING_CONFIG, "x", isFreeMode = false)
            .toUiMessage() as UiMessage.Res).id
        assertEquals(R.string.llm_error_prefix, free)
        assertEquals(R.string.llm_error_prefix, custom)
    }

    @Test
    fun everyFailureResultMapsToItsOwnResourceAndSuccessHasNone() {
        val expected = mapOf(
            VisionProbeResult.MODEL_UNSUPPORTED to R.string.vision_error_model_unsupported,
            VisionProbeResult.AUTH_ERROR to R.string.vision_error_auth,
            VisionProbeResult.QUOTA_ERROR to R.string.vision_error_quota,
            VisionProbeResult.RATE_LIMIT to R.string.vision_error_rate_limit,
            VisionProbeResult.NETWORK_ERROR to R.string.vision_error_network,
            VisionProbeResult.TIMEOUT to R.string.vision_error_timeout,
            VisionProbeResult.SERVER_ERROR to R.string.vision_error_server,
            VisionProbeResult.BAD_REQUEST to R.string.vision_error_bad_request,
            VisionProbeResult.MODEL_NOT_FOUND to R.string.vision_error_model_not_found,
            VisionProbeResult.RESPONSE_PARSE_ERROR to R.string.vision_error_response_parse,
            VisionProbeResult.INCOMPATIBLE to R.string.vision_error_incompatible,
            VisionProbeResult.UNKNOWN_ERROR to R.string.vision_error_unknown,
        )
        expected.forEach { (result, res) ->
            assertEquals(UiMessage.Res(res), result.toFailureUiMessage())
        }
        assertNull(VisionProbeResult.SUPPORTED.toFailureUiMessage())
        assertEquals(
            "所有失败结果都必须有独立提示",
            expected.keys + VisionProbeResult.SUPPORTED,
            VisionProbeResult.entries.toSet(),
        )
        assertTrue(expected.values.toSet().size == expected.size)
    }

    @Test
    fun actionStateCarriesMessageForDisplayLayer() {
        val state = com.example.smartstorage.presentation.settings.VisionActionState.Failed(
            UiMessage.Res(R.string.vision_error_network),
        )
        assertEquals(UiMessage.Res(R.string.vision_error_network), state.message)
    }
}
