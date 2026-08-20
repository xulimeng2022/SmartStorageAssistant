package com.example.smartstorage.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 带立体感按压动效与 Emoji 彩蛋的主要操作按钮。
 *
 * 按压时 scale 缩小至 0.95、阴影增大（产生“按下去”的立体感）；松开触发点击，
 * 同时在按钮上方弹出随机 Emoji（最多 3 个并发，各自独立动画并自动回收）。
 * 保留 M3 Button 默认涟漪。
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        label = "animatedButtonScale",
    )
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 8f else 2f,
        label = "animatedButtonElevation",
    )

    // 并发 Emoji 彩蛋（最多 3 个，id 去重，动画完成即移除）
    val emojis = remember { mutableStateListOf<Long>() }

    Box(modifier = modifier) {
        Button(
            onClick = {
                if (emojis.size < 3) {
                    emojis.add(System.nanoTime())
                }
                onClick()
            },
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    shape = RoundedCornerShape(50),
                    elevation = elevation.dp,
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            // Button 的内容槽为 RowScope，这里用 Box 提供 BoxScope 以便透传 content
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        }

        // Emoji 彩蛋层：叠加在按钮上方居中（外层 Box 提供 BoxScope）
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
