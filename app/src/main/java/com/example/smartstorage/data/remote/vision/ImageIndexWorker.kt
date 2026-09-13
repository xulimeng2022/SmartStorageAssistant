package com.example.smartstorage.data.remote.vision

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import com.example.smartstorage.data.remote.llm.LlmRequestException
import com.example.smartstorage.data.repository.ImageAiIndexRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val initialState = understandingRepository.state.value
        if (!initialState.enabled || initialState.capability != VisionCapabilityStatus.SUPPORTED) {
            return Result.success()
        }
        return try {
            indexRepository.reconcileActiveItems()
            var processed = 0
            while (true) {
                if (isStopped) throw CancellationException("图片索引任务已取消")
                val state = understandingRepository.state.value
                if (!state.enabled || state.capability != VisionCapabilityStatus.SUPPORTED) {
                    return Result.success()
                }
                val row = indexRepository.getQueued().firstOrNull() ?: break
                val file = File(row.imagePath)
                if (!file.exists()) {
                    val handled = failMissingImageFile(
                        claimProcessing = {
                            indexRepository.markProcessing(row.imagePath, row.generation)
                        },
                        markFailed = {
                            indexRepository.markFailed(
                                path = row.imagePath,
                                generation = row.generation,
                                hash = null,
                                errorKind = "FILE_MISSING",
                            )
                        },
                    )
                    if (!handled) break
                    processed++
                    setProgress(workDataOf(KEY_PROGRESS to processed))
                    continue
                }
                if (!indexRepository.markProcessing(row.imagePath, row.generation)) continue
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val hash = indexRepository.sha256(bytes)
                visionAnalyzer.analyze(bytes, mimeTypeFor(row.imagePath))
                    .onSuccess { analysis ->
                        val config = visionAnalyzer.currentConfig()
                        indexRepository.saveSuccess(
                            path = row.imagePath,
                            generation = row.generation,
                            hash = hash,
                            analysis = analysis,
                            provider = config.provider,
                            model = config.modelName,
                        )
                    }
                    .onFailure { error ->
                        val kind = (error as? LlmRequestException)?.kind?.name ?: "UNKNOWN"
                        indexRepository.markFailed(
                            path = row.imagePath,
                            generation = row.generation,
                            hash = hash,
                            errorKind = kind,
                        )
                    }
                processed++
                setProgress(workDataOf(KEY_PROGRESS to processed))
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
    }
}
/** 缺失图片先合法抢占，再标失败；并发变化时让本轮安全退出。 */
internal suspend fun failMissingImageFile(
    claimProcessing: suspend () -> Boolean,
    markFailed: suspend () -> Boolean,
): Boolean {
    if (!claimProcessing()) return false
    return markFailed()
}
