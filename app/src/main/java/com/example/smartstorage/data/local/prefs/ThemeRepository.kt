package com.example.smartstorage.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 独立 DataStore 实例：仅存放外观设置（主题模式 + 全局文字颜色），避免与其他配置混用
private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * 外观设置仓库：管理主题模式与全局文字颜色配置（DataStore 持久化）。
 *
 * - 主题模式：跟随系统 / 浅色 / 深色，MainActivity 订阅 [themeMode] 后全局即时生效。
 * - 全局文字颜色：默认 / 纯色 / 渐变，首页清单、物品详情、添加/编辑页输入框订阅
 *   [textColorConfig] 后实时生效（修复「必须重启 App 才变化」的问题）。
 */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // 主题模式键（int）
    private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
    // 文字颜色类型（String：DEFAULT / SOLID / GRADIENT）
    private val KEY_TEXT_COLOR_TYPE = stringPreferencesKey("text_color_type")
    // 纯色模式颜色（Long）
    private val KEY_TEXT_COLOR_SOLID = longPreferencesKey("text_color_solid")
    // 渐变起始 / 结束色（Long）
    private val KEY_TEXT_COLOR_GRADIENT_START = longPreferencesKey("text_color_gradient_start")
    private val KEY_TEXT_COLOR_GRADIENT_END = longPreferencesKey("text_color_gradient_end")
    // 渐变方向（String：HORIZONTAL / VERTICAL / DIAGONAL）
    private val KEY_TEXT_COLOR_DIRECTION = stringPreferencesKey("text_color_direction")
    // 旧版单一颜色迁移标记（非空即已完成，保证幂等）
    private val KEY_TEXT_COLOR_MIGRATED = stringPreferencesKey("text_color_migrated")

    // 仓库内部协程作用域：负责旧数据迁移与订阅 DataStore（单例，与应用同生命周期）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 全局文字颜色缓存：各 ViewModel 直接订阅此 StateFlow，DataStore 变化后实时更新
    private val _textColorConfig = MutableStateFlow(TextColorConfig())
    val textColorConfig: StateFlow<TextColorConfig> = _textColorConfig.asStateFlow()

    init {
        scope.launch {
            // 首次启动迁移旧版「输入文字颜色」→ 新全局颜色配置（纯色）
            migrateLegacyColorIfNeeded()
            // 订阅 DataStore：任何页面修改颜色后全局立即生效
            context.themeDataStore.data
                .map { prefs -> parseConfig(prefs) }
                .onEach { _textColorConfig.value = it }
                .launchIn(scope)
        }
    }

    /** 主题模式流：切换后所有页面立即生效，无需重启 App */
    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val value = prefs[KEY_THEME_MODE] ?: ThemeMode.FOLLOW_SYSTEM.value
        ThemeMode.entries.firstOrNull { it.value == value } ?: ThemeMode.FOLLOW_SYSTEM
    }

    /** 持久化主题模式 */
    suspend fun saveThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.value
        }
    }

    /** 持久化全局文字颜色配置（设置页每次选择即保存，实时生效） */
    suspend fun saveTextColorConfig(config: TextColorConfig) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_TEXT_COLOR_TYPE] = config.type.name
            prefs[KEY_TEXT_COLOR_SOLID] = config.solidColor
            prefs[KEY_TEXT_COLOR_GRADIENT_START] = config.gradientStart
            prefs[KEY_TEXT_COLOR_GRADIENT_END] = config.gradientEnd
            prefs[KEY_TEXT_COLOR_DIRECTION] = config.direction.name
            prefs[KEY_TEXT_COLOR_MIGRATED] = "1"
        }
    }

    /**
     * 从 DataStore 快照解析文字颜色配置（无记录时返回默认配置）。
     */
    private fun parseConfig(prefs: Preferences): TextColorConfig {
        val typeRaw = prefs[KEY_TEXT_COLOR_TYPE] ?: return TextColorConfig()
        return TextColorConfig(
            type = TextColorType.entries.firstOrNull { it.name == typeRaw } ?: TextColorType.DEFAULT,
            solidColor = prefs[KEY_TEXT_COLOR_SOLID] ?: 0xFF111111L,
            gradientStart = prefs[KEY_TEXT_COLOR_GRADIENT_START] ?: 0xFF111111L,
            gradientEnd = prefs[KEY_TEXT_COLOR_GRADIENT_END] ?: 0xFF1976D2L,
            direction = GradientDirection.entries.firstOrNull { it.name == prefs[KEY_TEXT_COLOR_DIRECTION] } ?: GradientDirection.HORIZONTAL,
        )
    }

    /**
     * 旧版迁移：AppPreferencesRepository（SharedPreferences "app_prefs"）曾存单一颜色
     * `input_text_color`（Int ARGB）。若 DataStore 尚无颜色配置且存在旧值，迁移为纯色配置。
     * 幂等：写入迁移标记后不再重复迁移。
     */
    private suspend fun migrateLegacyColorIfNeeded() {
        val migrated = context.themeDataStore.data.first()[KEY_TEXT_COLOR_MIGRATED]
        if (migrated != null) return
        val oldPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // 与 AppPreferencesRepository.KEY_INPUT_TEXT_COLOR / DEFAULT_INPUT_TEXT_COLOR 保持一致
        val oldColor = oldPrefs.getInt("input_text_color", 0xFF111111.toInt())
        context.themeDataStore.edit { prefs ->
            if (prefs[KEY_TEXT_COLOR_TYPE] == null) {
                prefs[KEY_TEXT_COLOR_TYPE] = TextColorType.SOLID.name
                prefs[KEY_TEXT_COLOR_SOLID] = oldColor.toLong()
            }
            prefs[KEY_TEXT_COLOR_MIGRATED] = "1"
        }
    }
}
