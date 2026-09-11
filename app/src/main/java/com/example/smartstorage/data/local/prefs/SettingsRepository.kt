package com.example.smartstorage.data.local.prefs

import com.example.smartstorage.R

import androidx.annotation.StringRes

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.smartstorage.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 智能解析配置。
 *
 * @property presetType 当前预设（DeepSeek / 通义千问 / OpenAI / 智谱AI / Moonshot / 百川 / MiniMax / Anthropic / Google / 自定义）
 * @property baseUrl 接口地址（OpenAI 兼容）
 * @property modelNameMap 各预设独立保存的模型版本（key = 预设 label，value = 已选模型）
 * @property apiKeyMap 各预设独立保存的 API Key（key = 预设 label，value = 对应 Key）
 * @property mode 使用模式：MODE_FREE（内置免费模型）/ MODE_CUSTOM（自定义配置）
 */
data class AiConfig(
    val presetType: String = LlmPreset.DEEPSEEK.label,
    val baseUrl: String = LlmPreset.DEEPSEEK.baseUrl,
    val modelNameMap: Map<String, String> = emptyMap(),
    val apiKeyMap: Map<String, String> = emptyMap(),
    val mode: String = MODE_FREE,
) {
    /** 当前预设的 API Key（按 presetType 从 map 中取出）。 */
    val apiKey: String get() = apiKeyMap[presetType] ?: ""

    /** 当前预设的模型版本（优先已保存值，否则用预设默认第一个模型）。 */
    val modelName: String get() =
        modelNameMap[presetType]
            ?: (LlmPreset.entries.firstOrNull { it.label == presetType }?.model ?: "")

    companion object {
        /** 免费模式：使用内置免费模型，无需配置。 */
        const val MODE_FREE = "FREE"

        /** 自定义模式：使用用户自行配置的 Base URL / 模型 / API Key。 */
        const val MODE_CUSTOM = "CUSTOM"
    }
}

/**
 * 免费模式内置模型（硅基流动 Qwen2.5-7B-Instruct，完全免费）。
 *
 * 固定 API 不写入 DataStore；API Key 仅从 local.properties 经 BuildConfig 注入。
 */
object FreeModel {
    /** 展示名称（用于设置页提示）。 */
    const val LABEL = "SiliconFlow Qwen3.5-4B"

    /** 接口地址（OpenAI 兼容）。 */
    const val BASE_URL = "https://api.siliconflow.cn/v1"

    /** 模型名称。 */
    const val MODEL = "Qwen/Qwen3.5-4B"

    /** API Key（本地构建时从 local.properties 的 SILICONFLOW_API_KEY 注入）。 */
    val API_KEY: String get() = BuildConfig.SILICONFLOW_API_KEY
}

/**
 * 大模型预设映射表。
 */
enum class LlmPreset(
    val label: String,
    @StringRes val displayNameRes: Int,
    val baseUrl: String,
    val models: List<String>,
    val isVerified: Boolean = true,
) {
    DEEPSEEK("DeepSeek", R.string.provider_deepseek, "https://api.deepseek.com/v1", listOf("deepseek-flash", "deepseek-v4-pro")),
    QWEN("通义千问", R.string.provider_qwen, "https://dashscope.aliyuncs.com/compatible-mode/v1", listOf("qwen3.8-max", "qwen3.8-flash", "qwen3.7-plus")),
    OPENAI("OpenAI", R.string.provider_openai, "https://api.openai.com/v1", listOf("gpt-6-astra", "gpt-5.6-terra", "gpt-5.6-luna")),
    ZHIPU("智谱AI", R.string.provider_zhipu, "https://open.bigmodel.cn/api/paas/v4", listOf("glm-5.3", "glm-5.3-flash", "glm-5.2")),
    MOONSHOT("Moonshot", R.string.provider_moonshot, "https://api.moonshot.cn/v1", listOf("kimi-k3", "kimi-k2.7-code-highspeed", "kimi-k2.6")),
    BAICHUAN("百川", R.string.provider_baichuan, "https://api.baichuan-ai.com/v1", listOf("Baichuan3-Turbo", "Baichuan4"), isVerified = false),
    MINIMAX("MiniMax", R.string.provider_minimax, "https://api.minimax.cn/v1", listOf("MiniMax-M3", "MiniMax-M2.7", "MiniMax-M2.5")),
    ANTHROPIC("Anthropic", R.string.provider_anthropic, "https://api.anthropic.com/v1", listOf("claude-fable-5-1", "claude-opus-5", "claude-sonnet-5")),
    GEMINI("Google", R.string.provider_google, "https://generativelanguage.googleapis.com/v1beta/openai/", listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.1-pro")),
    XAI("xAI", R.string.provider_xai, "https://api.x.ai/v1", listOf("grok-4.6", "grok-4.20")),
    MISTRAL("Mistral", R.string.provider_mistral, "https://api.mistral.ai/v1", listOf("mistral-medium-3-5", "mistral-small-2603", "mistral-large-3", "ministral-3-14b")),
    GROQ("Groq", R.string.provider_groq, "https://api.groq.com/openai/v1", listOf("openai/gpt-oss-120b", "qwen/qwen3.8-27b", "meta-llama/llama-4-maverick-17b-128e-instruct")),
    COHERE("Cohere", R.string.provider_cohere, "https://api.cohere.ai/compatibility/v1", listOf("command-a-plus-05-2026", "command-a-vision-07-2025", "command-a-reasoning-08-2025")),
    PERPLEXITY("Perplexity", R.string.provider_perplexity, "https://api.perplexity.ai", listOf("sonar-pro", "sonar-reasoning-pro", "sonar-deep-research")),
    CUSTOM("自定义", R.string.provider_custom, "", emptyList()),;

    /** 默认模型（列表第一个；自定义为空字符串）。 */
    val model: String get() = models.firstOrNull() ?: ""
}

/**
 * 设置仓库：
 * - API Key 用 EncryptedSharedPreferences 加密存储。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

    // DataStore 键
    private val KEY_PRESET = stringPreferencesKey("preset_type")
    private val KEY_BASE_URL = stringPreferencesKey("base_url")
    // 旧版单一模型名（仅用于迁移/兼容）
    private val KEY_MODEL = stringPreferencesKey("model_name")
    // 新版：各预设模型版本的 JSON 映射
    private val KEY_MODEL_MAP = stringPreferencesKey("model_name_map")
    // 使用模式（免费/自定义）
    private val KEY_MODE = stringPreferencesKey("mode")

    // 加密存储的各预设 API Key 缓存（跨进程读取，DataStore combine 需要）
    private val apiKeyFlow = MutableStateFlow(loadApiKeyMap())

    /** 保存配置：Base URL/模型存入 DataStore，API Key 按预设加密存储。 */
    suspend fun saveConfig(config: AiConfig) {
        // 各预设 API Key 加密存储
        saveApiKeyMap(config.apiKeyMap)
        apiKeyFlow.value = config.apiKeyMap

        // 其余配置存入 DataStore（模型版本按预设映射存储）
        context.dataStore.edit { prefs ->
            prefs[KEY_PRESET] = config.presetType
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_MODEL_MAP] = JSONObject().apply {
                config.modelNameMap.forEach { (k, v) -> put(k, v) }
            }.toString()
            // 兼容旧字段：保留当前预设的模型名
            prefs[KEY_MODEL] = config.modelName
            prefs[KEY_MODE] = config.mode
        }
    }

    /** 仅持久化使用模式（免费/自定义），不写任何 Base URL/模型/Key。 */
    suspend fun saveMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODE] = mode
        }
    }

    /** 读取配置流（DataStore 与加密的各预设 API Key 合并）。 */
    fun getConfigFlow(): Flow<AiConfig> =
        combine(context.dataStore.data, apiKeyFlow) { prefs, apiKeyMap ->
            AiConfig(
                presetType = prefs[KEY_PRESET] ?: LlmPreset.DEEPSEEK.label,
                baseUrl = prefs[KEY_BASE_URL] ?: LlmPreset.DEEPSEEK.baseUrl,
                modelNameMap = parseStringMap(prefs[KEY_MODEL_MAP]),
                apiKeyMap = apiKeyMap,
                mode = prefs[KEY_MODE] ?: AiConfig.MODE_FREE,
            )
        }

    /** 读取当前配置（挂起）；首次读取时迁移旧版单一 Key/模型名。 */
    suspend fun getConfig(): AiConfig {
        migrateLegacyApiKey()
        migrateLegacyModelName()
        return getConfigFlow().first()
    }

    /** 旧版本单一 api_key → 迁移为按预设保存（避免升级后 Key 丢失）。 */
    private suspend fun migrateLegacyApiKey() {
        val oldKey = encryptedPrefs().getString(KEY_API_KEY, null) ?: return
        if (oldKey.isBlank()) return
        val presetType = context.dataStore.data.first()[KEY_PRESET] ?: LlmPreset.DEEPSEEK.label
        val map = loadApiKeyMap()
        if (map[presetType].isNullOrBlank()) {
            saveApiKeyMap(map + (presetType to oldKey))
        }
        // 清理旧键，避免每次重复迁移
        encryptedPrefs().edit().remove(KEY_API_KEY).apply()
        apiKeyFlow.value = loadApiKeyMap()
    }

    /** 旧版本单一 model_name → 迁移为按预设保存（避免升级后模型版本丢失）。 */
    private suspend fun migrateLegacyModelName() {
        val prefs = context.dataStore.data.first()
        val map = parseStringMap(prefs[KEY_MODEL_MAP])
        if (map.isNotEmpty()) return
        val presetType = prefs[KEY_PRESET] ?: LlmPreset.DEEPSEEK.label
        val legacyModel = prefs[KEY_MODEL]
        if (legacyModel.isNullOrBlank()) return
        context.dataStore.edit { p ->
            p[KEY_MODEL_MAP] = JSONObject().apply { put(presetType, legacyModel) }.toString()
        }
    }

    /** JSON 字符串 → 字符串映射（解析失败返回空表）。 */
    private fun parseStringMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = HashMap<String, String>()
            obj.keys().forEach { key -> result[key] = obj.optString(key, "") }
            result
        }.getOrDefault(emptyMap())
    }

    /** 读取各预设 API Key 映射（JSON 加密存储）。 */
    private fun loadApiKeyMap(): Map<String, String> =
        parseStringMap(encryptedPrefs().getString(KEY_API_KEY_MAP, null))

    /** 保存各预设 API Key 映射（JSON 加密存储）。 */
    private fun saveApiKeyMap(map: Map<String, String>) {
        val json = JSONObject().apply {
            map.forEach { (k, v) -> put(k, v) }
        }.toString()
        encryptedPrefs().edit().putString(KEY_API_KEY_MAP, json).apply()
    }

    private fun encryptedPrefs(): SharedPreferences {
        // security-crypto 1.0：通过 MasterKeys 创建主密钥别名
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            ENCRYPTED_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        private const val DATASTORE_NAME = "ai_settings"
        private const val ENCRYPTED_PREFS_NAME = "ai_api_key_secure"
        // 旧版单一 Key（仅用于迁移）
        private const val KEY_API_KEY = "api_key"
        // 新版：各预设 Key 的 JSON 映射
        private const val KEY_API_KEY_MAP = "api_key_map"
    }
}
