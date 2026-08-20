package com.example.smartstorage.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 免费模型解析超时提示对话框：引导用户切换到自定义模式配置自己的 API Key。
 *
 * @param onGoToSettings 点击“去配置”：跳转设置页 AI 配置卡片
 * @param onLater 点击“稍后”/点外部/返回键：关闭
 */
@Composable
fun TimeoutDialog(
    onGoToSettings: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("解析超时") },
        text = {
            Text("免费模型响应较慢，建议切换到自定义模式，配置自己的 API Key 以获得更快的解析体验。")
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("去配置")
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text("稍后")
            }
        },
    )
}
