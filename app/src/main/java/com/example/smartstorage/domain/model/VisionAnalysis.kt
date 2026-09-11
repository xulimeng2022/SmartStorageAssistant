package com.example.smartstorage.domain.model

/** 三语言视觉字段；空值表示模型未识别。 */
data class LocalizedVisionFields(
    val zhHans: String = "",
    val zhHant: String = "",
    val en: String = "",
)

/** 单张图片的稳定结构化视觉结果。 */
data class VisionAnalysis(
    val objectTags: LocalizedVisionFields = LocalizedVisionFields(),
    val attributes: LocalizedVisionFields = LocalizedVisionFields(),
    val visibleText: LocalizedVisionFields = LocalizedVisionFields(),
    val description: LocalizedVisionFields = LocalizedVisionFields(),
    val confidence: Double = 0.0,
)

/** Top-K 二次复核等级。 */
enum class VisionMatchLevel { HIGH, MEDIUM, LOW, UNKNOWN }

/** Top-K 二次复核结果；reason 仍按当前界面语言生成。 */
data class VisionVerification(
    val candidateKey: String,
    val level: VisionMatchLevel,
    val reason: String,
    val confidence: Double,
)
