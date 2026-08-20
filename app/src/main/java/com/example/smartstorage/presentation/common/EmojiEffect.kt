package com.example.smartstorage.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Emoji 彩蛋字符集。 */
private val EMOJIS = listOf("🎉", "🎊", "⭐", "💫", "✨", "🌈", "🔥")

/**
 * 单个 Emoji 飘动动画：向上飘移约 -80dp 并淡出，约 0.8 秒后回调 [onFinished] 让上层移除（防泄漏）。
 */
@Composable
fun EmojiEffect(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val emoji = remember { EMOJIS.random() }
    val fontSize = remember { (24 + Random.nextInt(9)).sp }
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { alpha.animateTo(0f, tween(durationMillis = 800)) }
            launch { offsetY.animateTo(-80f, tween(durationMillis = 800)) }
        }
        delay(40)
        onFinished()
    }

    Text(
        text = emoji,
        fontSize = fontSize,
        modifier = modifier
            .offset(y = offsetY.value.dp)
            .graphicsLayer { this.alpha = alpha.value },
    )
}
