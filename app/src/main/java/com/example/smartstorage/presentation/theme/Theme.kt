package com.example.smartstorage.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.smartstorage.data.local.prefs.ThemeMode

// 浅色配色方案（沿用品牌靛蓝，保持既有浅色观感）
private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = OnBlue40,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = OnBlueContainerLight,
    secondary = BlueGrey40,
    onSecondary = OnBlueGrey40,
    secondaryContainer = BlueGreyContainerLight,
    onSecondaryContainer = OnBlueGreyContainerLight,
)

// 深色配色方案：微信式深灰层次（页面/卡片/填充/描边分层），强调色沿用品牌靛蓝；
// 关闭 Android 12+ 动态取色，保证深色观感稳定可控、与语言/浅色主题组合一致
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = OnBlue80,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = OnBlueContainerDark,
    secondary = BlueGrey80,
    onSecondary = OnBlueGrey80,
    secondaryContainer = BlueGreyContainerDark,
    onSecondaryContainer = OnBlueGreyContainerDark,
    // 语义层级：深灰表面，避免大面积纯黑纯白
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkBackground,
    surfaceBright = DarkSurfaceContainerHigh,
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    inverseSurface = DarkOnSurface,
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Blue40,
)

/**
 * 应用主题入口。
 *
 * @param themeMode 主题模式（跟随系统 / 浅色 / 深色）；由 MainActivity 订阅 ThemeRepository 传入，
 *                  切换后全局即时生效，无需重启 App
 * @param content 页面内容
 */
@Composable
fun SmartStorageTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    // 根据主题模式计算是否使用深色：跟随系统时用系统设置，浅色 / 深色强制固定
    val darkTheme = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // 不使用系统动态取色：深色按既定深灰层次渲染，浅色维持品牌色板
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
