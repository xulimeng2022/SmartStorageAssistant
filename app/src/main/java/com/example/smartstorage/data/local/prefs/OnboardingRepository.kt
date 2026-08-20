package com.example.smartstorage.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 独立 DataStore 实例：仅存放引导页相关标志，避免与 AI 配置混用
private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

/**
 * 引导页仓库：持久化“是否首次启动”标志。
 */
@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // 首次启动标志键（默认 true：从未写入即视为首次启动）
    private val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")

    /** 是否首次启动（未完成引导时为 true）。 */
    val isFirstLaunch: Flow<Boolean> = context.onboardingDataStore.data
        .map { prefs -> prefs[KEY_IS_FIRST_LAUNCH] ?: true }

    /** 完成引导：写入 false，之后不再展示引导页。 */
    suspend fun completeOnboarding() {
        context.onboardingDataStore.edit { prefs ->
            prefs[KEY_IS_FIRST_LAUNCH] = false
        }
    }
}