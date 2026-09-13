package com.example.smartstorage.presentation.add

import com.example.smartstorage.domain.model.BatchDraftItem

/** 合并预览可勾选照片与默认选中照片。 */
internal data class BatchMergePhotoCandidates(
    val availablePhotoPaths: List<String>,
    val selectedPhotoPaths: List<String>,
)

/**
 * 计算合并预览照片：
 * - 候选为选中草稿照片并集加上照片池中未分配给任何草稿的照片；
 * - 默认只选中来源草稿已有的照片；
 * - 全部按应用私有绝对路径精确去重，不比较文件名或内容。
 */
internal fun resolveBatchMergePhotoCandidates(
    drafts: List<BatchDraftItem>,
    selectedUids: Set<Long>,
    currentImagePaths: List<String>,
): BatchMergePhotoCandidates {
    val allAssignedPhotoPaths = drafts.flatMap { it.photoPaths }.toSet()
    val selectedDraftPhotoPaths = drafts
        .filter { it.uid in selectedUids }
        .flatMap { it.photoPaths }
        .distinct()
    val unassignedPhotoPaths = currentImagePaths.filterNot { it in allAssignedPhotoPaths }

    return BatchMergePhotoCandidates(
        availablePhotoPaths = (selectedDraftPhotoPaths + unassignedPhotoPaths).distinct(),
        selectedPhotoPaths = selectedDraftPhotoPaths,
    )
}
