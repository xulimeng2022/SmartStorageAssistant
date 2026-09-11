package com.example.smartstorage.data.remote.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 视觉能力缓存指纹测试：对不同 Provider / Base URL / 模型 / 模式敏感，忽略尾斜杠与空白差异。 */
class VisionRuntimeConfigTest {

    private fun config(
        baseUrl: String = "https://api.deepseek.com/v1",
        modelName: String = "deepseek-flash",
        provider: String = "DeepSeek",
        isFreeMode: Boolean = false,
        apiKey: String = "test-key",
    ) = VisionRuntimeConfig(
        baseUrl = baseUrl,
        modelName = modelName,
        apiKey = apiKey,
        provider = provider,
        isFreeMode = isFreeMode,
    )

    @Test
    fun trailingSlashAndWhitespaceAreNormalized() {
        assertEquals(config().fingerprint, config(baseUrl = "https://api.deepseek.com/v1/").fingerprint)
        assertEquals(config().fingerprint, config(modelName = " deepseek-flash ").fingerprint)
    }

    @Test
    fun providerChangeInvalidatesFingerprint() {
        assertNotEquals(config().fingerprint, config(provider = "OpenAI").fingerprint)
    }

    @Test
    fun baseUrlChangeInvalidatesFingerprint() {
        assertNotEquals(
            config().fingerprint,
            config(baseUrl = "https://api.deepseek.com/v2").fingerprint,
        )
    }

    @Test
    fun modelChangeInvalidatesFingerprint() {
        assertNotEquals(config().fingerprint, config(modelName = "deepseek-v4-pro").fingerprint)
        // 旧 alias 与新推荐模型是不同指纹：能力缓存不会串用
        assertNotEquals(
            config(modelName = "deepseek-flash").fingerprint,
            config(modelName = "deepseek-v4-flash").fingerprint,
        )
    }

    @Test
    fun modeChangeInvalidatesFingerprint() {
        assertNotEquals(config().fingerprint, config(isFreeMode = true).fingerprint)
    }

    @Test
    fun fingerprintIsStableAcrossCalls() {
        val cfg = config()
        assertEquals(cfg.fingerprint, cfg.fingerprint)
        assertEquals(64, cfg.fingerprint.length)
    }
}
