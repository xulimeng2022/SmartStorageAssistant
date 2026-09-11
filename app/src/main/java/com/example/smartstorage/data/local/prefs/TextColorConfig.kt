package com.example.smartstorage.data.local.prefs

import androidx.annotation.StringRes

import com.example.smartstorage.R

/**
 * 文字颜色类型：默认（跟随主题）/ 纯色 / 渐变。
 */
enum class TextColorType(@StringRes val labelRes: Int) {
    DEFAULT(R.string.text_color_default),
    SOLID(R.string.text_color_solid),
    GRADIENT(R.string.text_color_gradient),
}

/**
 * 渐变方向：水平 / 垂直 / 对角（45°）。
 */
enum class GradientDirection(@StringRes val labelRes: Int) {
    HORIZONTAL(R.string.gradient_horizontal),
    VERTICAL(R.string.gradient_vertical),
    DIAGONAL(R.string.gradient_diagonal),
}

/**
 * 全局文字颜色配置：首页清单、物品详情、添加/编辑页输入框统一使用。
 *
 * @property type 颜色类型（默认跟随主题 / 纯色 / 渐变）
 * @property solidColor 纯色模式的 ARGB 颜色值
 * @property gradientStart 渐变模式的起始色（ARGB）
 * @property gradientEnd 渐变模式的结束色（ARGB）
 * @property direction 渐变方向（仅渐变模式生效）
 */
data class TextColorConfig(
    val type: TextColorType = TextColorType.DEFAULT,
    val solidColor: Long = 0xFF111111L,
    val gradientStart: Long = 0xFF111111L,
    val gradientEnd: Long = 0xFF1976D2L,
    val direction: GradientDirection = GradientDirection.HORIZONTAL,
)
