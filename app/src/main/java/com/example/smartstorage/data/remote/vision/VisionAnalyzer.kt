package com.example.smartstorage.data.remote.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.SettingsRepository
import com.example.smartstorage.data.remote.llm.LlmErrorClassifier
import com.example.smartstorage.data.remote.llm.LlmErrorKind
import com.example.smartstorage.data.remote.llm.LlmRequestException
import com.example.smartstorage.domain.model.VisionAnalysis
import com.example.smartstorage.domain.model.VisionMatchLevel
import com.example.smartstorage.domain.model.VisionVerification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视觉能力探测结果：细分失败原因，避免把鉴权/网络/额度/解析问题误报为模型不支持。
 */
enum class VisionProbeResult {
    SUPPORTED,
    MODEL_UNSUPPORTED,
    AUTH_ERROR,
    QUOTA_ERROR,
    RATE_LIMIT,
    NETWORK_ERROR,
    TIMEOUT,
    SERVER_ERROR,
    BAD_REQUEST,
    MODEL_NOT_FOUND,
    RESPONSE_PARSE_ERROR,
    INCOMPATIBLE,
    UNKNOWN_ERROR,
}

data class VisionRuntimeConfig(
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val provider: String,
    val isFreeMode: Boolean,
) {
    val fingerprint: String
        get() {
            val raw = listOf(
                provider,
                baseUrl.trim().trimEnd('/'),
                modelName.trim(),
                if (isFreeMode) "FREE" else "CUSTOM",
                VisionAnalyzer.PROTOCOL_VERSION,
                VisionAnalyzer.PROBE_VERSION,
            ).joinToString("|")
            return MessageDigest.getInstance("SHA-256")
                .digest(raw.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
}

/** 视觉分析入口：统一处理免费/自定义配置、探测、图片分析和 Top-K 复核。 */
@Singleton
class VisionAnalyzer @Inject constructor(
    private val transport: VisionTransport,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun currentConfig(): VisionRuntimeConfig {
        val config = settingsRepository.getConfig()
        return if (config.mode == AiConfig.MODE_FREE) {
            VisionRuntimeConfig(
                baseUrl = FreeModel.BASE_URL,
                modelName = FreeModel.MODEL,
                apiKey = FreeModel.API_KEY,
                provider = FREE_PROVIDER_ID,
                isFreeMode = true,
            )
        } else {
            VisionRuntimeConfig(
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                apiKey = config.apiKey,
                provider = config.presetType,
                isFreeMode = false,
            )
        }
    }

    suspend fun probe(): VisionProbeResult = probe(currentConfig())

    suspend fun probe(config: VisionRuntimeConfig): VisionProbeResult {
        if (config.baseUrl.isBlank() || config.modelName.isBlank() || config.apiKey.isBlank()) {
            return VisionProbeResult.BAD_REQUEST
        }
        val bytes = syntheticProbePng()
        return withContext(Dispatchers.IO) {
            try {
                val content = transport.postVisionCompletion(
                    baseUrl = config.baseUrl,
                    modelName = config.modelName,
                    apiKey = config.apiKey,
                    systemPrompt = PROBE_SYSTEM_PROMPT,
                    userText = PROBE_USER_PROMPT,
                    imageBytes = bytes,
                    mimeType = "image/png",
                    maxTokens = PROBE_MAX_TOKENS,
                )
                VisionProbeEvaluator.evaluate(content)
            } catch (e: LlmRequestException) {
                VisionProbeEvaluator.classify(e)
            } catch (_: SocketTimeoutException) {
                VisionProbeResult.TIMEOUT
            } catch (_: IOException) {
                VisionProbeResult.NETWORK_ERROR
            } catch (_: IllegalArgumentException) {
                VisionProbeResult.BAD_REQUEST
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                VisionProbeResult.UNKNOWN_ERROR
            }
        }
    }

    suspend fun analyze(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<VisionAnalysis> =
        runCatching {
            withContext(Dispatchers.IO) {
                val config = currentConfig()
                check(config.baseUrl.isNotBlank() && config.modelName.isNotBlank() && config.apiKey.isNotBlank()) {
                    "视觉模型配置不完整"
                }
                val content = transport.postVisionCompletion(
                    baseUrl = config.baseUrl,
                    modelName = config.modelName,
                    apiKey = config.apiKey,
                    systemPrompt = ANALYSIS_SYSTEM_PROMPT,
                    userText = ANALYSIS_USER_PROMPT,
                    imageBytes = imageBytes,
                    mimeType = mimeType,
                )
                VisionJsonParser.parseAnalysis(content) ?: error("视觉模型返回格式不兼容")
            }
        }

    suspend fun verify(question: String, imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<VisionVerification> =
        runCatching {
            withContext(Dispatchers.IO) {
                val config = currentConfig()
                val content = transport.postVisionCompletion(
                    baseUrl = config.baseUrl,
                    modelName = config.modelName,
                    apiKey = config.apiKey,
                    systemPrompt = VERIFY_SYSTEM_PROMPT,
                    userText = "用户搜索问题：$question",
                    imageBytes = imageBytes,
                    mimeType = mimeType,
                )
                VisionJsonParser.parseVerification(content) ?: error("复核结果格式不兼容")
            }
        }

    private fun syntheticProbePng(): ByteArray {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.RED
        canvas.drawCircle(38f, 40f, 24f, paint)
        paint.color = Color.BLUE
        canvas.drawRect(70f, 66f, 112f, 108f, paint)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    companion object {
        /** 免费模式稳定 Provider 标识（用于能力指纹，不随展示名变化）。 */
        private const val FREE_PROVIDER_ID = "siliconflow"

        /** OpenAI 兼容协议版本：协议升级时变更，使旧能力缓存自动失效。 */
        const val PROTOCOL_VERSION = "openai-chat-completions-v1"

        /** 探测算法版本：探测协议变更时递增，使旧能力缓存自动失效。 */
        const val PROBE_VERSION = "vision-probe-v2"

        /** 探测输出很短，独立的小上限避免推理模型把额度耗在长输出上。 */
        private const val PROBE_MAX_TOKENS = 256

        private const val PROBE_SYSTEM_PROMPT = "你是视觉能力检测器。请观察图片并简短回答，不要解释。"
        private const val PROBE_USER_PROMPT = "用一句话回答这张图片里有哪些颜色和形状。"
        private const val ANALYSIS_SYSTEM_PROMPT = "你是严谨的图片内容结构化分析器。只返回合法 JSON，不推测看不到的内容。"
        private const val ANALYSIS_USER_PROMPT = "分析图片中与收纳物品有关的内容。返回 objects、attributes、visibleText、description、confidence；objects/attributes/visibleText 均为含 zh-Hans、zh-Hant、en 的数组，description 为三语言字符串。"
        private const val VERIFY_SYSTEM_PROMPT = "你是视觉搜索候选复核器。只返回 JSON：{\"candidateId\":\"\",\"match\":\"HIGH|MEDIUM|LOW\",\"reason\":{\"zh-Hans\":\"\",\"zh-Hant\":\"\",\"en\":\"\"},\"confidence\":0.0}"
    }
}
