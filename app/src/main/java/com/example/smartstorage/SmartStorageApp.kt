package com.example.smartstorage

import android.app.Application
import android.content.Context
import com.example.smartstorage.data.local.prefs.AppLanguage
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口：启用 Hilt 依赖注入。
 */
@HiltAndroidApp
class SmartStorageApp : Application() {
    /** 应用级 Context 也跟随用户选择的语言，供 ViewModel 的 Toast/消息使用。 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }
}