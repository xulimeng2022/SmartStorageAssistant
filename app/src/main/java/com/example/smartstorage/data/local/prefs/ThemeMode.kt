package com.example.smartstorage.data.local.prefs

import androidx.annotation.StringRes

import com.example.smartstorage.R

/**
 * 主题模式枚举：跟随系统 / 强制浅色 / 强制深色。
 *
 * @property value 持久化到 DataStore 的整数值（缺省 0 = 跟随系统）
 * @property label 设置页展示名称
 */
enum class ThemeMode(val value: Int, @StringRes val labelRes: Int) {
    FOLLOW_SYSTEM(0, R.string.theme_follow_system),
    LIGHT(1, R.string.theme_light),
    DARK(2, R.string.theme_dark),
}
