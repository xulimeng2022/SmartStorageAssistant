package com.example.smartstorage.presentation.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** 色相环预设颜色：每 30° 一个纯色相，共 12 段 */
private val hueRingColors: List<Color> =
    (0 until 360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }

/** 环宽占外半径的比例 */
private const val RING_WIDTH_RATIO = 0.14f

/**
 * 自绘 HSV 色环颜色选择器：
 * - 外环：色相（SweepGradient 圆环）
 * - 中心圆盘：饱和度（中心白 → 边缘纯色相），叠加亮度黑色遮罩
 * - 底部滑条：亮度（Value）0..1
 *
 * 点击 / 拖拽选色，实时回调 [onColorChange]；外部传入 [color] 变化时自动同步内部状态。
 */
@Composable
fun HsvColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 由外部颜色反推当前 HSV，供绘制与初始选中标记使用
    val hsv = remember(color) {
        val arr = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 外部颜色变化（如点击预设色）时同步内部 HSV，保证选中标记与预览一致
    LaunchedEffect(color) {
        val arr = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), arr)
        hue = arr[0]
        sat = arr[1]
        value = arr[2]
    }

    // 把触摸位置换算为 HSV 并回调：盘内改色相+饱和度，环上只改色相
    fun onTouch(pos: Offset) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val dx = pos.x - center.x
        val dy = pos.y - center.y
        val dist = hypot(dx, dy)
        val outerRadius = min(canvasSize.width, canvasSize.height) / 2f
        val innerRadius = outerRadius * (1f - RING_WIDTH_RATIO)
        // atan2 返回 -PI..PI，转成 0..360 的色相角
        val angle = ((atan2(dy, dx) * 180.0 / PI) + 360.0) % 360.0
        when {
            // 中心圆盘内：色相 + 饱和度（径向）
            dist <= innerRadius -> {
                hue = angle.toFloat()
                sat = (dist / innerRadius).coerceIn(0f, 1f)
            }
            // 色相环上：仅改色相
            dist <= outerRadius -> {
                hue = angle.toFloat()
            }
            else -> return
        }
        onColorChange(Color.hsv(hue, sat, value))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { pos -> onTouch(pos) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onTouch(change.position)
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f
            val ringWidth = outerRadius * RING_WIDTH_RATIO
            val innerRadius = outerRadius - ringWidth

            // 外环：色相环
            drawCircle(
                brush = Brush.sweepGradient(colors = hueRingColors),
                radius = outerRadius,
                center = center,
                style = Stroke(width = ringWidth),
            )
            // 中心圆盘：白 → 纯色相（饱和度径向渐变）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.hsv(hue, 1f, 1f)),
                    center = center,
                    radius = innerRadius,
                ),
                radius = innerRadius,
                center = center,
            )
            // 亮度遮罩：中心透明 → 边缘黑色（alpha = 1 - value）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 1f - value)),
                    center = center,
                    radius = innerRadius,
                ),
                radius = innerRadius,
                center = center,
            )

            // 选中标记：环上（角度 = 当前色相）
            val ringAngle = hue * PI / 180.0
            val ringMarker = Offset(
                center.x + innerRadius * cos(ringAngle).toFloat(),
                center.y + innerRadius * sin(ringAngle).toFloat(),
            )
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = ringMarker)
            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = 6.dp.toPx(),
                center = ringMarker,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            // 选中标记：盘内（半径 = 当前饱和度）
            val diskAngle = hue * PI / 180.0
            val diskRadius = sat * innerRadius
            val diskMarker = Offset(
                center.x + diskRadius * cos(diskAngle).toFloat(),
                center.y + diskRadius * sin(diskAngle).toFloat(),
            )
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = diskMarker)
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 5.dp.toPx(),
                center = diskMarker,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // 亮度滑条（Value）
        Slider(
            value = value,
            onValueChange = { v ->
                value = v
                onColorChange(Color.hsv(hue, sat, v))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
