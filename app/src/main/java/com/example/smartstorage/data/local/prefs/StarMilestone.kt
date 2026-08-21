package com.example.smartstorage.data.local.prefs

/** GitHub Star 提醒里程碑：历史累计添加数达到这些值时各提醒一次 */
val STAR_MILESTONES = listOf(5, 15, 30)

/** GitHub 仓库地址（点击「去 GitHub 点 Star」跳转） */
const val STAR_REPO_URL = "https://github.com/xulimeng2022/SmartStorageAssistant"

/** 稍后提醒间隔：24 小时（毫秒） */
const val STAR_REMIND_LATER_MILLIS = 24L * 60 * 60 * 1000

/**
 * Star 里程碑状态快照：用于添加成功后检查是否需要弹出提醒。
 *
 * @property totalAdded 历史累计添加物品数（删除也累计，仅新增成功时 +1）
 * @property reminded 已永久完成提醒的里程碑集合
 * @property remindLaterTimestamp 稍后提醒时间戳（0 表示无待提醒）
 */
data class StarMilestoneState(
    val totalAdded: Int = 0,
    val reminded: Set<Int> = emptySet(),
    val remindLaterTimestamp: Long = 0L,
) {
    /**
     * 计算当前需要展示提醒的里程碑（从小到大取第一个，无则返回 null）。
     *
     * @param now 当前时间戳，用于判断「稍后提醒」是否已到期
     */
    fun pendingMilestone(now: Long = System.currentTimeMillis()): Int? {
        // 稍后提醒未到期：暂不弹任何提醒
        if (remindLaterTimestamp > now) return null
        return STAR_MILESTONES.firstOrNull { milestone ->
            totalAdded >= milestone && milestone !in reminded
        }
    }
}
