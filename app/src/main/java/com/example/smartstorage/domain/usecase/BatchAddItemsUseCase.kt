package com.example.smartstorage.domain.usecase

import com.example.smartstorage.domain.model.BatchDuplicateChoice
import com.example.smartstorage.domain.model.BatchDraftItem
import com.example.smartstorage.domain.model.Item
import javax.inject.Inject

/** 每件物品最多保留的照片数量（与添加页/详情页一致）。 */
private const val MAX_IMAGES_PER_ITEM = 9

/** 批量保存的统计结果。 */
data class BatchAddOutcome(
    /** 成功新增的件数（含“新建记录”决策）。 */
    val added: Int = 0,
    /** 成功更新的件数（“更新旧记录”决策）。 */
    val updated: Int = 0,
    /** 用户选择跳过的件数（“跳过此物品”决策）。 */
    val skipped: Int = 0,
    /** 保存失败的草稿条目 uid（名称缺失 / 写入异常），供 UI 按 uid 保留并重试。 */
    val failedUids: List<Long> = emptyList(),
    /**
     * 本次保存中以“原路径”直存、从而被某条记录引用的草稿照片路径。
     * 调用方（ViewModel）在部分失败重试时应把它们并入 alreadyClaimedPaths，
     * 避免重试条目再次直存同一路径导致两条记录共享文件。
     */
    val consumedOriginals: List<String> = emptyList(),
)

/**
 * 批量新增/更新物品用例：对勾选草稿逐条处理，单条失败不影响其它条目。
 *
 * 同名记录通过挂起的 [resolveDuplicate] 回调让 UI 弹窗决策。
 * 照片归属由每条草稿自带的 [BatchDraftItem.photoPaths] 决定（同一张照片可分配给多个草稿）；
 * 保存时保证“任何绝对路径至多被一条记录引用”：路径未被占用则原路径直存，
 * 已被占用（本次更早条目 / [alreadyClaimedPaths] 中已落库的路径 / 目标旧记录已有路径）则调用
 * [copyFile] 生成独立副本，避免删除或清理一条记录时影响其它记录。
 */
class BatchAddItemsUseCase @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val getItemByNameUseCase: GetItemByNameUseCase,
) {

    /**
     * 执行批量保存。
     *
     * @param drafts 全部草稿条目（含 uid 与各自照片归属）
     * @param selected 勾选（要保存）的草稿 uid 集合
     * @param alreadyClaimedPaths 已被既有记录占用的路径（编辑模式下原记录照片 + 此前成功批次直存的路径），
     *   命中时改用 [copyFile] 复制而非直存
     * @param resolveDuplicate 遇到同名旧记录时的用户决策回调（挂起等待 UI）
     * @param copyFile 为指定照片路径生成一份独立副本（返回新路径）
     */
    suspend fun execute(
        drafts: List<BatchDraftItem>,
        selected: Set<Long>,
        alreadyClaimedPaths: Set<String> = emptySet(),
        resolveDuplicate: suspend (existing: Item, draft: BatchDraftItem) -> BatchDuplicateChoice,
        copyFile: suspend (String) -> String,
    ): BatchAddOutcome {
        var added = 0
        var updated = 0
        var skipped = 0
        val failed = mutableListOf<Long>()
        val consumed = mutableListOf<String>()
        // 已被某条记录占用的绝对路径：同一次保存内互斥，且跨重试由 alreadyClaimedPaths 延续
        val claimed = alreadyClaimedPaths.toMutableSet()
        val now = System.currentTimeMillis()

        drafts.filter { it.uid in selected }.sortedBy { it.uid }.forEach { draft ->
            val uid = draft.uid
            val name = draft.name.trim()
            // 名称缺失：记入失败名单，由 UI 提示补充后重试
            if (name.isEmpty()) {
                failed += uid
                return@forEach
            }
            try {
                val existing = getItemByNameUseCase(name)
                // 决策：无重名直接新增；有重名由用户选择更新/新建/跳过
                val decision = existing?.let { resolveDuplicate(it, draft) }
                if (decision == BatchDuplicateChoice.SKIP) {
                    skipped++
                    return@forEach
                }
                val targetExisting = if (decision == BatchDuplicateChoice.UPDATE_EXISTING) existing else null
                // 更新旧记录时其旧照片继续由该记录持有，纳入占用集合
                val keepOldPaths = targetExisting?.imagePaths.orEmpty()
                keepOldPaths.forEach { claimed += it }

                // 计算本记录最终照片：旧路径保留 + 新照片去重并按占用规则直存/复制
                val newImages = mutableListOf<String>()
                draft.photoPaths.distinct().forEach { p ->
                    // 旧记录已有该路径则不重复添加（同一条记录内同一路径只出现一次）
                    if (p in keepOldPaths) return@forEach
                    if (claimed.contains(p)) {
                        // 已被其它记录占用：复制独立副本，避免共享文件
                        newImages += copyFile(p)
                    } else {
                        // 首次占用：原路径直存并记录，供重试时避免再次直存
                        claimed += p
                        consumed += p
                        newImages += p
                    }
                }
                val imagePaths = (keepOldPaths + newImages).take(MAX_IMAGES_PER_ITEM)

                if (targetExisting == null) {
                    // 无重名 或 用户选择“新建记录”：直接插入（不携带旧记录照片）
                    addItemUseCase(
                        Item(
                            name = name,
                            location = draft.location.trim(),
                            description = draft.description.trim(),
                            imagePaths = imagePaths,
                        ),
                    )
                    added++
                } else {
                    // 更新旧记录：保留 id/createdAt 与旧照片，位置/备注取本次解析/编辑结果
                    updateItemUseCase(
                        targetExisting.copy(
                            location = draft.location.trim(),
                            description = draft.description.trim(),
                            imagePaths = imagePaths,
                            updatedAt = now,
                        ),
                    )
                    updated++
                }
            } catch (e: Exception) {
                // 单条失败记入名单，不影响其它条目继续保存
                failed += uid
            }
        }
        return BatchAddOutcome(
            added = added,
            updated = updated,
            skipped = skipped,
            failedUids = failed,
            consumedOriginals = consumed,
        )
    }
}
