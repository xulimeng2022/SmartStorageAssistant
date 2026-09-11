package com.example.smartstorage.domain.model

/** 首页视觉索引命中的展示信息。 */
data class VisualMatch(
    val item: Item,
    val imagePath: String,
    val evidence: String,
    val confidence: Double,
    val score: Double,
) {
    val key: String get() = "${item.id}:$imagePath"
}

/** Top-K 二次复核状态，仅存在于当前搜索结果会话。 */
data class VisualVerificationState(
    val candidateKey: String,
    val level: VisionMatchLevel = VisionMatchLevel.UNKNOWN,
    val reason: String = "",
    val confidence: Double = 0.0,
    val failed: Boolean = false,
)

/** 搜索结果合并类型。 */
enum class SearchMatchSource { NORMAL, VISUAL, BOTH }
