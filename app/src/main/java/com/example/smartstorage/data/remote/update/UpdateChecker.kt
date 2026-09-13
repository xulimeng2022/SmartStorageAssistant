package com.example.smartstorage.data.remote.update

import javax.inject.Inject
import javax.inject.Singleton

/** 检查更新：比较当前版本与 GitHub 稳定版本。 */
@Singleton
class UpdateChecker @Inject constructor(
    private val transport: UpdateTransport,
) {
    suspend fun check(currentVersion: String): UpdateCheckResult {
        val release = transport.fetchLatestStable().getOrElse {
            return UpdateCheckResult.Failed(it.message ?: "network")
        }
        val newer = UpdateVersionComparator.isNewer(release.versionName, currentVersion)
            ?: return UpdateCheckResult.Failed("invalid version")
        return if (newer) {
            UpdateCheckResult.Available(currentVersion, release)
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }
    }
}