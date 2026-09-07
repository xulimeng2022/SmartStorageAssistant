package com.example.smartstorage.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

/**
 * AI 解析失败统一引导对话框（两出口，添加页与首页共用）。
 *
 * 触发条件：本次 AI/请求最终失败（超时、配置缺失、鉴权/额度/限流、网络/服务端、模型返回为空或不可解析等）。
 * - 「去配置 API」：停止本次引导，跳转应用内 AI 配置（由上层保证保留草稿与输入）；
 * - 「稍后」：关闭弹窗；上层按“是否已本地降级”衔接——未降级则执行本地规则降级，已降级则保留结果；
 *   系统返回键/点外部与「稍后」同行为，不丢输入、不自动重试、不自动保存。
 *
 * @param bodyRes 正文资源（添加/编辑页与首页搜索两套文案）
 * @param onGoToSettings 点击「去配置 API」
 * @param onLater 点击「稍后」/返回键/点外部
 */
@Composable
fun AiParseFailedDialog(
    @StringRes bodyRes: Int,
    onGoToSettings: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.ai_fail_title)) },
        text = {
            Text(stringResource(bodyRes))
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(stringResource(R.string.ai_fail_configure))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.ai_fail_later))
            }
        },
    )
}
