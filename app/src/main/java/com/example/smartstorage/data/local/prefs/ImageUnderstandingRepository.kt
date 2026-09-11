package com.example.smartstorage.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class VisionCapabilityStatus {
    UNKNOWN,
    SUPPORTED,
    UNSUPPORTED,
    INCOMPATIBLE,
    ERROR,
}

enum class ImageIndexJobState {
    IDLE,
    ACTIVE,
    PAUSED,
}

data class ImageUnderstandingState(
    val enabled: Boolean = false,
    val capability: VisionCapabilityStatus = VisionCapabilityStatus.UNKNOWN,
    val capabilityFingerprint: String? = null,
    val jobState: ImageIndexJobState = ImageIndexJobState.IDLE,
    val lastErrorKind: String? = null,
)

/** AI 图片理解隐私开关、探测结果和索引任务控制状态。 */
@Singleton
class ImageUnderstandingRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(readState())

    val state: StateFlow<ImageUnderstandingState> = mutableState.asStateFlow()

    suspend fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        refresh()
    }

    /**
     * 只持久化成功的探测结果；失败（网络/鉴权/额度/解析等）不写入缓存。
     *
     * 服务商可能升级同一 model alias 的能力（如 DeepSeek Flash 路由到 V4.1），
     * 负结果一旦持久化就会造成“永远无法开启”；因此失败只在本次会话内存中展示。
     */
    suspend fun saveCapability(fingerprint: String, status: VisionCapabilityStatus) {
        if (status != VisionCapabilityStatus.SUPPORTED) {
            invalidateCapability()
            return
        }
        prefs.edit()
            .putString(KEY_FINGERPRINT, fingerprint)
            .putString(KEY_CAPABILITY, status.name)
            .remove(KEY_ERROR)
            .apply()
        refresh()
    }

    suspend fun invalidateCapability() {
        prefs.edit()
            .remove(KEY_FINGERPRINT)
            .putString(KEY_CAPABILITY, VisionCapabilityStatus.UNKNOWN.name)
            .remove(KEY_ERROR)
            .apply()
        refresh()
    }

    suspend fun setJobState(jobState: ImageIndexJobState) {
        prefs.edit().putString(KEY_JOB_STATE, jobState.name).apply()
        refresh()
    }

    private fun readState(): ImageUnderstandingState {
        // 旧版本可能持久化过 UNSUPPORTED/INCOMPATIBLE/ERROR 负结果；读取时一律视为未检测。
        val supported = prefs.getString(KEY_CAPABILITY, null) == VisionCapabilityStatus.SUPPORTED.name
        return ImageUnderstandingState(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            capability = if (supported) VisionCapabilityStatus.SUPPORTED else VisionCapabilityStatus.UNKNOWN,
            capabilityFingerprint = if (supported) prefs.getString(KEY_FINGERPRINT, null) else null,
            jobState = prefs.getString(KEY_JOB_STATE, null)
                ?.let { runCatching { ImageIndexJobState.valueOf(it) }.getOrNull() }
                ?: ImageIndexJobState.IDLE,
            // 失败原因不再持久化，仅由设置页本次会话状态展示。
            lastErrorKind = null,
        )
    }

    private fun refresh() {
        mutableState.value = readState()
    }

    companion object {
        private const val PREFS_NAME = "image_understanding"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CAPABILITY = "capability"
        private const val KEY_FINGERPRINT = "capability_fingerprint"
        private const val KEY_JOB_STATE = "job_state"
        private const val KEY_ERROR = "last_error"
    }
}
