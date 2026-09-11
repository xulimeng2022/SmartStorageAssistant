package com.example.smartstorage.data.remote.vision

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.smartstorage.data.local.prefs.ImageIndexJobState
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import com.example.smartstorage.data.remote.llm.LlmRequestException
import com.example.smartstorage.data.repository.ImageAiIndexRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.File

@HiltWorker
class ImageIndexWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val indexRepository: ImageAiIndexRepository,
    private val understandingRepository: ImageUnderstandingRepository,
    private val visionAnalyzer: VisionAnalyzer,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val state = understandingRepository.state.value
        if (!state.enabled || state.capability != VisionCapabilityStatus.SUPPORTED) {
            return Result.success()
        }
        return try {
            indexRepository.reconcileActiveItems()
            val queued = indexRepository.getQueued()
            if (queued.isEmpty()) return Result.success()
            var processed = 0
            queued.forEach { row ->
                if (isStopped) throw CancellationException("图片索引任务已取消")
                val current = understandingRepository.state.value
                if (!current.enabled || current.capability != VisionCapabilityStatus.SUPPORTED) {
                    return Result.success()
                }
                val file = File(row.imagePath)
                if (!file.exists()) {
                    indexRepository.markFailed(row.imagePath, null, "FILE_MISSING")
                    processed++
                    setProgress(workDataOf(KEY_PROGRESS to processed, KEY_TOTAL to queued.size))
                    return@forEach
                }
                indexRepository.markProcessing(row.imagePath)
                val bytes = file.readBytes()
                val hash = indexRepository.sha256(bytes)
                visionAnalyzer.analyze(bytes, mimeTypeFor(row.imagePath))
                    .onSuccess { analysis ->
                        val config = visionAnalyzer.currentConfig()
                        indexRepository.saveSuccess(
                            path = row.imagePath,
                            hash = hash,
                            analysis = analysis,
                            provider = config.provider,
                            model = config.modelName,
                        )
                    }
                    .onFailure { error ->
                        val kind = (error as? LlmRequestException)?.kind?.name ?: "UNKNOWN"
                        indexRepository.markFailed(row.imagePath, hash, kind)
                    }
                processed++
                setProgress(workDataOf(KEY_PROGRESS to processed, KEY_TOTAL to queued.size))
            }
            Result.success()
        } catch (e: CancellationException) {
            indexRepository.restoreProcessing()
            throw e
        } catch (_: Exception) {
            indexRepository.restoreProcessing()
            Result.retry()
        }
    }

    private fun mimeTypeFor(path: String): String =
        if (path.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"

    companion object {
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
    }
}
