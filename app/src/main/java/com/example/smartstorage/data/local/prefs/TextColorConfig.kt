package com.example.smartstorage.data.local.prefs

/**
 * 文字颜色类型：默认（跟随主题）/ 纯色 / 渐变。
 */
enum class TextColorType(val label: String) {
    DEFAULT("默认"),
    SOLID("纯色"),
    GRADIENT("渐变"),
}

/**
 * 渐变方向：水平 / 垂直 / 对角（45°）。
 */
enum class GradientDirection(val label: String) {
    HORIZONTAL("水平"),
    VERTICAL("垂直"),
    DIAGONAL("对角"),
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
