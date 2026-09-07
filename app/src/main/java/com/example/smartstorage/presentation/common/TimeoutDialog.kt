package com.example.smartstorage.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

/**
 * 免费模式解析超时/不可用提示对话框：给出三个明确出口，点击后必须真正结束等待并继续/停止任务。
 *
 * @param onUseLocal 点击「本地识别继续（稍后配置）」：结束等待，用本地规则继续处理本次输入
 * @param onGoToSettings 点击「去配置 API」：停止本次任务并跳设置页 AI 配置（保留输入与既有 API 配置）
 * @param onCancel 点击「取消」/点外部/返回键：停止任务，保留输入，仅手动填写
 */
@Composable
fun TimeoutDialog(
    onUseLocal: () -> Unit,
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.timeout_title)) },
        text = {
            Text(stringResource(R.string.timeout_text))
        },
        confirmButton = {
            Row {
                TextButton(onClick = onUseLocal) {
                    Text(stringResource(R.string.timeout_local))
                }
                TextButton(onClick = onGoToSettings) {
                    Text(stringResource(R.string.timeout_configure))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
