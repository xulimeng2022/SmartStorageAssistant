package com.example.smartstorage.presentation.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.smartstorage.data.local.prefs.GradientDirection
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.TextColorType

/**
 * 全局文字颜色渲染工具：把 [TextColorConfig] 转成可直接用于 Text / OutlinedTextField 的 [TextStyle]。
 *
 * 首页清单、物品详情、添加/编辑页输入框三处共用，保证「设置页改色 → 全局实时生效」表现一致。
 *
 * @param config 全局文字颜色配置
 * @param baseStyle 基础样式（字号 / 字重等，颜色会被覆盖）
 * @param defaultColor 默认类型使用的颜色（通常为主题 onSurface）
 */
fun textColorStyle(
    config: TextColorConfig,
    baseStyle: TextStyle,
    defaultColor: Color,
): TextStyle = when (config.type) {
    TextColorType.DEFAULT -> baseStyle.copy(color = defaultColor)
    TextColorType.SOLID -> baseStyle.copy(color = Color(config.solidColor))
    TextColorType.GRADIENT -> baseStyle.copy(
        brush = gradientBrush(config),
    )
}

/** 按配置的方向生成渐变画刷（水平 / 垂直 / 对角 45°） */
fun gradientBrush(config: TextColorConfig): Brush {
    val start = Color(config.gradientStart)
    val end = Color(config.gradientEnd)
    return when (config.direction) {
        GradientDirection.HORIZONTAL -> Brush.horizontalGradient(listOf(start, end))
        GradientDirection.VERTICAL -> Brush.verticalGradient(listOf(start, end))
        GradientDirection.DIAGONAL -> Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }
}
