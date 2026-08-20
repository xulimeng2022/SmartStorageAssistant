package com.example.smartstorage.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用外观偏好仓库：持久化“输入文字颜色”等个性化设置。
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读取输入文字颜色（ARGB），默认深色。 */
    fun getInputTextColor(): Int = prefs.getInt(KEY_INPUT_TEXT_COLOR, DEFAULT_INPUT_TEXT_COLOR)

    /** 保存输入文字颜色。 */
    fun setInputTextColor(color: Int) {
        prefs.edit().putInt(KEY_INPUT_TEXT_COLOR, color).apply()
    }

    /** 是否显示首页“搜索小贴士”弹窗（默认显示）。 */
    fun getSearchTipsEnabled(): Boolean = prefs.getBoolean(KEY_SEARCH_TIPS_ENABLED, true)

    /** 设置首页“搜索小贴士”弹窗是否显示。 */
    fun setSearchTipsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_TIPS_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_INPUT_TEXT_COLOR = "input_text_color"
        private const val KEY_SEARCH_TIPS_ENABLED = "search_tips_enabled"

        /** 默认输入文字颜色：深黑，与浅色提示文字明确区分。 */
        const val DEFAULT_INPUT_TEXT_COLOR = 0xFF111111.toInt()
    }
}