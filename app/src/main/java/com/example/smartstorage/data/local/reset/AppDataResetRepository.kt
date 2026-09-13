package com.example.smartstorage.data.local.reset

import android.content.Context
import androidx.work.WorkManager
import com.example.smartstorage.data.local.AppDatabase
import com.example.smartstorage.data.local.image.ImageStorage
import com.example.smartstorage.data.local.image.deleteDirectoryContents
import com.example.smartstorage.data.local.prefs.AppLanguage
import com.example.smartstorage.data.local.prefs.AppPreferencesRepository
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.OnboardingRepository
import com.example.smartstorage.data.local.prefs.SettingsRepository
import com.example.smartstorage.data.local.prefs.StarMilestoneRepository
import com.example.smartstorage.data.local.prefs.ThemeRepository
import com.example.smartstorage.data.remote.vision.ImageIndexingCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 完全删除本地 App 数据的逐项结果。 */
data class ResetReport(
    val workersStopped: Boolean,
    val databaseCleared: Boolean,
    val filesCleared: Boolean,
    val preferencesCleared: Boolean,
    val errors: List<String>,
) {
    val isComplete: Boolean get() = workersStopped && databaseCleared && filesCleared && preferencesCleared && errors.isEmpty()
}

/** 汇总清空结果；任一布尔结果为 false 时记录失败而不是静默成功。 */
internal fun recordClearResults(
    results: List<Boolean>,
    label: String,
    errors: MutableList<String>,
): Boolean {
    val success = results.all { it }
    if (!success) errors += "$label:false"
    return success
}

/** 统一清空 App 自有数据；不删除系统相册、外部备份或远端服务数据。 */
@Singleton
class AppDataResetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
    private val settingsRepository: SettingsRepository,
    private val themeRepository: ThemeRepository,
    private val onboardingRepository: OnboardingRepository,
    private val starMilestoneRepository: StarMilestoneRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val imageUnderstandingRepository: ImageUnderstandingRepository,
) {
    suspend fun resetAll(): ResetReport = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val workersStopped = runCatching {
            val operation = WorkManager.getInstance(context).cancelUniqueWork(ImageIndexingCoordinator.WORK_NAME)
            operation.result.get(5, TimeUnit.SECONDS)
        }.onFailure { errors += "worker:${it.javaClass.simpleName}" }.isSuccess

        val databaseCleared = runCatching {
            database.clearAllTables()
        }.onFailure { errors += "database:${it.javaClass.simpleName}" }.isSuccess

        val filesCleared = runCatching {
            recordClearResults(
                results = listOf(
                    imageStorage.clearAllManagedImages(),
                    deleteDirectoryContents(context.cacheDir),
                ),
                label = "files",
                errors = errors,
            )
        }.onFailure { errors += "files:${it.javaClass.simpleName}" }.getOrDefault(false)

        val preferencesCleared = runCatching {
            recordClearResults(
                results = listOf(
                    settingsRepository.clearAll(),
                    themeRepository.clearAll(),
                    onboardingRepository.clearAll(),
                    starMilestoneRepository.clearAll(),
                    appPreferencesRepository.clearAll(),
                    imageUnderstandingRepository.clearAll(),
                    AppLanguage.clear(context),
                ),
                label = "preferences",
                errors = errors,
            )
        }.onFailure { errors += "preferences:${it.javaClass.simpleName}" }.getOrDefault(false)

        ResetReport(
            workersStopped = workersStopped,
            databaseCleared = databaseCleared,
            filesCleared = filesCleared,
            preferencesCleared = preferencesCleared,
            errors = errors,
        )
    }
}
