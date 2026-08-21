package com.example.smartstorage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 订阅主题模式：切换后无需重启 App，全局立即换肤
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            SmartStorageTheme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
}
