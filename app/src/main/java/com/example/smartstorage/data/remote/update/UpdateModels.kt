package com.example.smartstorage.data.remote.update

/** 已发布的稳定版本信息。 */
data class UpdateRelease(
    val versionName: String,
    val notes: String,
    val downloadUrl: String,
    val releaseUrl: String,
)

/** 检查更新结果；检查失败不得误报为“已是最新”。 */
sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Available(val currentVersion: String, val release: UpdateRelease) : UpdateCheckResult
    data class Failed(val reason: String) : UpdateCheckResult
}

/** 语义版本比较器；只接受 x.y.z，拒绝预发布后缀。 */
object UpdateVersionComparator {
    private val stableVersion = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")

    fun parse(version: String): List<Int>? {
        val parts = stableVersion.matchEntire(version.trim())?.groupValues?.drop(1) ?: return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    fun isNewer(candidate: String, current: String): Boolean? {
        val left = parse(candidate) ?: return null
        val right = parse(current) ?: return null
        for (i in 0..2) {
            if (left[i] != right[i]) return left[i] > right[i]
        }
        return false
    }
}