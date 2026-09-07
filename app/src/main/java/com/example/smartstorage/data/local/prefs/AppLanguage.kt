package com.example.smartstorage.data.local.prefs

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * 应用语言助手：用户可在「跟随系统 / 简体中文 / 繁體中文 / English」间切换。
 *
 * 用 SharedPreferences 存语言码（空串=跟随系统；zh / zh-rTW / en），
 * 以便 [wrap] 能在 Activity.attachBaseContext（Hilt 注入之前）同步读取并应用，
 * 语言与深浅主题相互独立、互不覆盖。
 */
object AppLanguage {
    private const val PREFS_NAME = "app_lang"
    private const val KEY_CODE = "lang_code"

    /** 支持的语言码：空串（跟随系统）/ 简体 / 繁体 / English。 */
    val SUPPORTED_CODES = listOf("", "zh", "zh-rTW", "en")

    /** 读取当前语言码（默认跟随系统）。 */
    fun getCode(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CODE, "") ?: ""

    /** 保存语言码（调用方随后触发 Activity.recreate 立即生效）。 */
    fun setCode(context: Context, code: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CODE, code)
            .apply()
    }

    /**
     * 按已选语言包装 Context：跟随系统时原样返回（由系统资源决定，不受支持的语言回退到默认简体）；
     * 手动选择时把 Locale 应用到返回的 Context，供 Activity.attachBaseContext 使用。
     */
    fun wrap(context: Context): Context {
        val code = getCode(context)
        if (code.isBlank()) return context
        val locale = when (code) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "zh-rTW" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            else -> return context
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
