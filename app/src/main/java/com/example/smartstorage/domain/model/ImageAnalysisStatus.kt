package com.example.smartstorage.domain.model

/** 图片 AI 索引状态。 */
enum class ImageAnalysisStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    OUTDATED,
}
