package com.example.smartstorage.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 带 Emoji 彩蛋的图标按钮：点击触发随机 Emoji 飘动（最多 3 个并发，动画结束自动回收），保留 M3 涟漪。
 */
@Composable
fun EmojiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val emojis = remember { mutableStateListOf<Long>() }

    // 注意：内部 IconButton 不使用 fillMaxSize，避免在 wrap-content 的 Row 里被撑满最大高度
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                if (emojis.size < 3) {
                    emojis.add(System.nanoTime())
                }
                onClick()
            },
            enabled = enabled,
        ) {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                content()
            }
        }

        // Emoji 彩蛋层：叠加在按钮上方居中
        emojis.forEach { id ->
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmojiEffect(onFinished = { emojis.remove(id) })
            }
        }
    }
}
