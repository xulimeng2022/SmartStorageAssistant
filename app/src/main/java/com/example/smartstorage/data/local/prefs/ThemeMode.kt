package com.example.smartstorage.data.local.prefs

/**
 * 主题模式枚举：跟随系统 / 强制浅色 / 强制深色。
 *
 * @property value 持久化到 DataStore 的整数值（缺省 0 = 跟随系统）
 * @property label 设置页展示名称
 */
enum class ThemeMode(val value: Int, val label: String) {
    FOLLOW_SYSTEM(0, "跟随系统"),
    LIGHT(1, "浅色"),
    DARK(2, "深色"),
}
