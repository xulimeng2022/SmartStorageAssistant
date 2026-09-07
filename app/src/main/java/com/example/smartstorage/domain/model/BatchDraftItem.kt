package com.example.smartstorage.domain.model

/**
 * 批量录入中的可编辑草稿条目：识别结果 + 用户编辑 + 照片归属。
 *
 * 以 [uid] 作为稳定标识（与列表下标解耦），删除/勾选/编辑/照片归属一律按 uid 操作，
 * 避免删除条目或重排后照片归属串到其它物品。照片路径指向本次会话的照片池
 * （表单 currentImagePaths 中的文件），同一张照片可分配给多个草稿条目。
 */
data class BatchDraftItem(
    val uid: Long,
    val name: String,
    val location: String,
    val description: String,
    val photoPaths: List<String> = emptyList(),
)

/** 批量草稿的纯函数操作（无 Android 依赖，便于单元测试）。 */
object BatchDraftOps {

    /** 按 uid 修改某条草稿的名称/地点/备注；找不到时原样返回。 */
    fun update(
        items: List<BatchDraftItem>,
        uid: Long,
        name: String,
        location: String,
        description: String,
    ): List<BatchDraftItem> = items.map {
        if (it.uid == uid) it.copy(name = name, location = location, description = description) else it
    }

    /** 按 uid 删除某条草稿（不删除照片文件，照片仍留在池中可再分配）。 */
    fun remove(items: List<BatchDraftItem>, uid: Long): List<BatchDraftItem> =
        items.filterNot { it.uid == uid }

    /**
     * 切换某条草稿对某张照片的归属。
     *
     * @param assign true 表示把 [photoPath] 加入该条草稿（已存在则忽略）；false 表示移除。
     */
    fun setPhoto(
        items: List<BatchDraftItem>,
        uid: Long,
        photoPath: String,
        assign: Boolean,
    ): List<BatchDraftItem> = items.map { draft ->
        if (draft.uid != uid) {
            draft
        } else if (assign) {
            if (photoPath in draft.photoPaths) draft else draft.copy(photoPaths = draft.photoPaths + photoPath)
        } else {
            draft.copy(photoPaths = draft.photoPaths.filterNot { it == photoPath })
        }
    }

    /** 从照片池移除某路径时，把所有草稿条目中对该路径的引用一并剥离，避免悬空引用。 */
    fun detachPath(items: List<BatchDraftItem>, photoPath: String): List<BatchDraftItem> =
        items.map { draft ->
            if (photoPath in draft.photoPaths) {
                draft.copy(photoPaths = draft.photoPaths.filterNot { it == photoPath })
            } else {
                draft
            }
        }
}
