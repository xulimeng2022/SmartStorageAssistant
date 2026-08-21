package com.example.smartstorage.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// 独立 DataStore 实例：仅存放 GitHub Star 里程碑提醒状态
private val Context.starDataStore by preferencesDataStore(name = "star_milestone")

/**
 * GitHub Star 里程碑提醒仓库（DataStore 持久化）：
 * - 维护历史累计添加物品数 total_added（每次新增成功 +1，删除不减少）
 * - 维护各里程碑是否已永久提醒（star_reminded_5 / 15 / 30）
 * - 维护「稍后提醒」时间戳（star_remind_later_timestamp，0 表示无待提醒）
 */
@Singleton
class StarMilestoneRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val KEY_TOTAL_ADDED = intPreferencesKey("total_added")
    private val KEY_REMIND_LATER = longPreferencesKey("star_remind_later_timestamp")

    private fun remindedKey(milestone: Int) = booleanPreferencesKey("star_reminded_$milestone")

    /** 历史累计添加数 +1（仅在新增物品成功后调用） */
    suspend fun incrementTotalAdded() {
        context.starDataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_ADDED] ?: 0
            prefs[KEY_TOTAL_ADDED] = current + 1
        }
    }

    /** 读取里程碑状态快照（用于添加成功后检查） */
    suspend fun readState(): StarMilestoneState {
        val prefs = context.starDataStore.data.first()
        return StarMilestoneState(
            totalAdded = prefs[KEY_TOTAL_ADDED] ?: 0,
            reminded = STAR_MILESTONES.filter { prefs[remindedKey(it)] == true }.toSet(),
            remindLaterTimestamp = prefs[KEY_REMIND_LATER] ?: 0L,
        )
    }

    /** 标记某里程碑已永久提醒（点击「去 GitHub 点 Star」后调用），并清除稍后提醒 */
    suspend fun markReminded(milestone: Int) {
        context.starDataStore.edit { prefs ->
            prefs[remindedKey(milestone)] = true
            prefs[KEY_REMIND_LATER] = 0L
        }
    }

    /** 设置「稍后提醒」时间戳（点「稍后提醒」或关闭弹窗时调用） */
    suspend fun setRemindLater(timestamp: Long) {
        context.starDataStore.edit { prefs ->
            prefs[KEY_REMIND_LATER] = timestamp
        }
    }
}
