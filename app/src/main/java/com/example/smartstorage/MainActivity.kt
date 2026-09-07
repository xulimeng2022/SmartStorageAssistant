package com.example.smartstorage

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color as AndroidColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.Context
import com.example.smartstorage.data.local.prefs.AppLanguage
import com.example.smartstorage.data.local.prefs.ThemeMode
import com.example.smartstorage.data.local.prefs.ThemeRepository
import com.example.smartstorage.presentation.MainScreen
import com.example.smartstorage.presentation.theme.SmartStorageTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主界面 Activity：承载 Compose 页面，并订阅全局主题模式（切换后立即生效）。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 外观设置仓库：读取用户选择的主题模式（跟随系统 / 浅色 / 深色）
    @Inject
    lateinit var themeRepository: ThemeRepository

    /** 语言切换后重建 Activity：把用户选择的应用语言应用到界面资源。 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 冷启动首帧窗口背景与应用主题一致，避免深色模式下白色闪屏
        val cachedMode = runBlocking { themeRepository.themeMode.first() }
        val darkNow = when (cachedMode) {
            ThemeMode.FOLLOW_SYSTEM -> (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        window.decorView.setBackgroundColor(
            if (darkNow) AndroidColor.parseColor("#111111") else AndroidColor.parseColor("#FFFFFF"),
        )

        setContent {
            // 订阅主题模式：切换后无需重启 App，全局立即换肤
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            SmartStorageTheme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
}
