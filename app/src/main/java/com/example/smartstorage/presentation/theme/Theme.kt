package com.example.smartstorage.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.smartstorage.data.local.prefs.ThemeMode

// 浅色配色方案
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

// 深色配色方案
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = OnBlue80,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = OnBlueContainerDark,
    secondary = BlueGrey80,
    onSecondary = OnBlueGrey80,
    secondaryContainer = BlueGreyContainerDark,
    onSecondaryContainer = OnBlueGreyContainerDark,
)

/**
 * 应用主题入口。
 *
 * @param themeMode 主题模式（跟随系统 / 浅色 / 深色）；由 MainActivity 订阅 ThemeRepository 传入，
 *                  切换后全局即时生效，无需重启 App
 * @param dynamicColor 是否启用 Android 12+ 动态取色（默认开启）
 * @param content 页面内容
 */
@Composable
fun SmartStorageTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // 根据主题模式计算是否使用深色：跟随系统时用系统设置，浅色 / 深色强制固定
    val darkTheme = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        // Android 12 及以上且开启动态取色时，使用系统壁纸配色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
