package com.example.smartstorage.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 首页“搜索小贴士”弹窗（首页与设置页共用）。
 *
 * @param doNotRemind 是否勾选“不再提示”（true = 以后不再弹出）
 * @param onDoNotRemindChange 勾选状态变化
 * @param onConfirm 点击“知道了”/点外部/返回键关闭
 */
@Composable
fun SearchTipsDialog(
    doNotRemind: Boolean,
    onDoNotRemindChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("🔍 搜索小贴士") },
        text = {
            Column {
                Text(
                    text = "① 支持按 名称 / 地点 / 备注 搜索，可输入，或点 🎤 用键盘语音说出要找的物品。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "② 说完点搜索条内的 ✨ 智能解析，AI 会自动提取名称、地点、备注，精确定位物品。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "例如：“昨天放在桌子上的一支笔” → 名称「笔」· 地点「桌子上」",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // “不再提示”勾选 + 说明
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = doNotRemind,
                        onCheckedChange = onDoNotRemindChange,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "不再提示",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "可在设置页再次查看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("知道了")
            }
        },
    )
}
