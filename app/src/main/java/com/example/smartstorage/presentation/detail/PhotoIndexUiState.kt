package com.example.smartstorage.presentation.detail

import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import com.example.smartstorage.domain.model.ImageAnalysisStatus

/** 详情页单张照片的索引状态。 */
enum class PhotoIndexStatus {
    NOT_CREATED,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    OUTDATED,
}

/** 单图索引按钮允许执行的 UI 动作。 */
enum class PhotoIndexAction {
    CREATE_OR_RETRY,
    REFRESH,
    DELETE,
    NONE,
}

fun PhotoIndexStatus.toPhotoIndexAction(): PhotoIndexAction = when (this) {
    PhotoIndexStatus.NOT_CREATED,
    PhotoIndexStatus.FAILED,
    -> PhotoIndexAction.CREATE_OR_RETRY
    PhotoIndexStatus.OUTDATED -> PhotoIndexAction.REFRESH
    PhotoIndexStatus.SUCCESS -> PhotoIndexAction.DELETE
    PhotoIndexStatus.PENDING,
    PhotoIndexStatus.PROCESSING,
    -> PhotoIndexAction.NONE
}

data class PhotoIndexUiState(
    val status: PhotoIndexStatus,
    val errorKind: String? = null,
)

fun ImageAiIndexEntity?.toPhotoIndexUiState(): PhotoIndexUiState {
    if (this == null || !requested) return PhotoIndexUiState(PhotoIndexStatus.NOT_CREATED)
    val status = when (this.status) {
        ImageAnalysisStatus.PENDING.name -> PhotoIndexStatus.PENDING
        ImageAnalysisStatus.PROCESSING.name -> PhotoIndexStatus.PROCESSING
        ImageAnalysisStatus.SUCCESS.name -> PhotoIndexStatus.SUCCESS
        ImageAnalysisStatus.FAILED.name -> PhotoIndexStatus.FAILED
        ImageAnalysisStatus.OUTDATED.name -> PhotoIndexStatus.OUTDATED
        else -> PhotoIndexStatus.NOT_CREATED
    }
    return PhotoIndexUiState(status, errorKind)
}