package com.example.smartstorage.data.remote.vision

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartstorage.data.local.prefs.ImageIndexJobState
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import com.example.smartstorage.data.repository.ImageAiIndexRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 图片视觉索引任务控制入口。 */
@Singleton
class ImageIndexingCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val indexRepository: ImageAiIndexRepository,
    private val understandingRepository: ImageUnderstandingRepository,
) {
    suspend fun startHistory() {
        understandingRepository.setJobState(ImageIndexJobState.ACTIVE)
        indexRepository.reconcileActiveItems()
        enqueue()
    }

    suspend fun resume() = startHistory()

    suspend fun pause() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        indexRepository.restoreProcessing()
        understandingRepository.setJobState(ImageIndexJobState.PAUSED)
    }

    suspend fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        indexRepository.restoreProcessing()
        understandingRepository.setJobState(ImageIndexJobState.IDLE)
    }

    suspend fun retryFailed() {
        indexRepository.retryFailed()
        understandingRepository.setJobState(ImageIndexJobState.ACTIVE)
        enqueue()
    }

    suspend fun clearAll() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        indexRepository.clearAll()
        understandingRepository.setJobState(ImageIndexJobState.IDLE)
    }

    /** 用户明确请求单图索引；未开启/不支持时不做网络请求。 */
    suspend fun requestIndex(itemId: Long, path: String): Boolean {
        val state = understandingRepository.state.value
        if (!state.enabled || state.capability != VisionCapabilityStatus.SUPPORTED) return false
        val changed = indexRepository.requestIndex(itemId, path)
        if (changed) {
            understandingRepository.setJobState(ImageIndexJobState.ACTIVE)
            enqueue()
        }
        return changed
    }

    /** 用户明确删除单图索引；保留墓碑，旧 Worker 由 generation 拦截。 */
    suspend fun disableIndex(path: String): Boolean = indexRepository.disableIndex(path)

    /** 保存/编辑后同步图片索引状态；仅对用户选中的照片发起分析。 */
    suspend fun syncItemImages(itemId: Long, paths: List<String>, requestedPaths: Set<String>) {
        val state = understandingRepository.state.value
        val allowed = if (state.enabled && state.capability == VisionCapabilityStatus.SUPPORTED) {
            requestedPaths
        } else {
            emptySet()
        }
        indexRepository.syncItemImages(itemId, paths, allowed)
        if (allowed.isNotEmpty()) {
            understandingRepository.setJobState(ImageIndexJobState.ACTIVE)
            enqueue()
        }
    }

    suspend fun onNewImagesSaved() {
        if (!understandingRepository.state.value.enabled) return
        if (understandingRepository.state.value.jobState == ImageIndexJobState.IDLE) {
            understandingRepository.setJobState(ImageIndexJobState.ACTIVE)
        }
        enqueue()
    }

    private fun enqueue() {
        val request = OneTimeWorkRequestBuilder<ImageIndexWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    companion object {
        const val WORK_NAME = "image_ai_index"
    }
}