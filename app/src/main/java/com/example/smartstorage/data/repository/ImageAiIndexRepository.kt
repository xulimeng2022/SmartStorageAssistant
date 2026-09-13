package com.example.smartstorage.data.repository

import com.example.smartstorage.data.local.dao.ImageAiIndexDao
import com.example.smartstorage.data.local.dao.ItemDao
import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import com.example.smartstorage.domain.model.ImageAnalysisStatus
import com.example.smartstorage.domain.model.LocalizedVisionFields
import com.example.smartstorage.domain.model.VisionAnalysis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** 视觉索引进度。 */
data class ImageIndexProgress(
    val total: Int = 0,
    val success: Int = 0,
    val pending: Int = 0,
    val processing: Int = 0,
    val failed: Int = 0,
    val outdated: Int = 0,
) {
    val finished: Int get() = success
}

/** 图片视觉索引仓库：持久化、状态迁移和 JSON 编解码。 */
@Singleton
class ImageAiIndexRepository @Inject constructor(
    private val indexDao: ImageAiIndexDao,
    private val itemDao: ItemDao,
) {
    fun observeAll(): Flow<List<ImageAiIndexEntity>> = indexDao.observeAll()

    fun observeSuccessful(): Flow<List<ImageAiIndexEntity>> = indexDao.observeSuccessful()

    /** 观察单件物品的全部索引行，包含停用墓碑供 UI 展示“未创建”。 */
    fun observeByItem(itemId: Long): Flow<List<ImageAiIndexEntity>> = indexDao.observeByItem(itemId)

    fun observeProgress(): Flow<ImageIndexProgress> = indexDao.observeAll().map { rows ->
        ImageIndexProgress(
            total = rows.size,
            success = rows.count { it.status == ImageAnalysisStatus.SUCCESS.name },
            pending = rows.count { it.status == ImageAnalysisStatus.PENDING.name },
            processing = rows.count { it.status == ImageAnalysisStatus.PROCESSING.name },
            failed = rows.count { it.status == ImageAnalysisStatus.FAILED.name },
            outdated = rows.count { it.status == ImageAnalysisStatus.OUTDATED.name },
        )
    }

    suspend fun getByItem(itemId: Long): List<ImageAiIndexEntity> = indexDao.getByItem(itemId)

    suspend fun getByPath(path: String): ImageAiIndexEntity? = indexDao.getByPath(path)

    suspend fun getQueued(): List<ImageAiIndexEntity> = indexDao.getQueued()

    suspend fun getFailed(): List<ImageAiIndexEntity> = indexDao.getFailed()

    suspend fun getProcessing(): List<ImageAiIndexEntity> = indexDao.getProcessing()

    /**
     * 用户明确请求单张图片索引。
     *
     * @return false 表示物品不存在、路径不属于该物品或索引已经有效。
     */
    suspend fun requestIndex(itemId: Long, path: String): Boolean {
        val item = itemDao.getById(itemId) ?: return false
        if (path !in item.imagePaths) return false
        val existing = indexDao.getByPath(path)
        if (existing == null) {
            indexDao.upsert(
                ImageAiIndexEntity(
                    imagePath = path,
                    itemId = itemId,
                    status = ImageAnalysisStatus.PENDING.name,
                    requested = true,
                    generation = 0L,
                ),
            )
            return true
        }
        if (existing.itemId != itemId) return false
        if (existing.requested) return false
        return indexDao.requestExisting(path, itemId) > 0
    }

    /** 用户明确删除单图索引：保留墓碑，不删除物品和照片文件。 */
    suspend fun disableIndex(path: String): Boolean = indexDao.disableByPath(path) > 0

    /** 物理删除索引行，仅在照片本身已删除或物品永久删除时使用。 */
    suspend fun deleteIndex(path: String) = indexDao.deleteByPath(path)

    suspend fun markProcessing(path: String, generation: Long): Boolean =
        indexDao.markProcessing(path, generation) > 0

    suspend fun restoreProcessing() = indexDao.restoreProcessing()

    suspend fun retryFailed() = indexDao.retryFailed()

    suspend fun clearAll() = indexDao.deleteAll()

    suspend fun deleteItem(itemId: Long) = indexDao.deleteByItem(itemId)

    suspend fun markFailed(path: String, generation: Long, hash: String?, errorKind: String): Boolean =
        indexDao.markFailed(path, generation, hash, errorKind, System.currentTimeMillis()) > 0

    suspend fun saveSuccess(
        path: String,
        generation: Long,
        hash: String,
        analysis: VisionAnalysis,
        provider: String,
        model: String,
    ): Boolean = indexDao.markSuccess(
        path = path,
        generation = generation,
        hash = hash,
        objects = encodeFields(analysis.objectTags),
        attributes = encodeFields(analysis.attributes),
        visibleText = encodeFields(analysis.visibleText),
        description = encodeFields(analysis.description),
        searchText = buildSearchText(analysis),
        confidence = analysis.confidence.coerceIn(0.0, 1.0),
        provider = provider,
        model = model,
        indexVersion = INDEX_VERSION,
        analyzedAt = System.currentTimeMillis(),
    ) > 0

    /**
     * 保存/编辑物品后同步索引行。
     *
     * [requestedPaths] 是用户希望建立索引的照片；未列入且仍存在的路径会写成 requested=0 墓碑。
     * 已不在 [paths] 中的行会被物理删除，避免悬空引用。
     */
    suspend fun syncItemImages(itemId: Long, paths: List<String>, requestedPaths: Set<String>) {
        val normalizedPaths = paths.distinct()
        val existing = indexDao.getByItem(itemId).associateBy { it.imagePath }
        normalizedPaths.forEach { path ->
            val row = existing[path]
            val requested = path in requestedPaths
            when {
                row == null && requested -> indexDao.upsert(
                    ImageAiIndexEntity(
                        imagePath = path,
                        itemId = itemId,
                        status = ImageAnalysisStatus.PENDING.name,
                        requested = true,
                    ),
                )
                row == null -> Unit
                requested && !row.requested -> indexDao.requestExisting(path, itemId)
                !requested && row.requested -> indexDao.disableByPath(path)
            }
        }
        existing.keys.filterNot { it in normalizedPaths }.forEach { indexDao.deleteByPath(it) }
    }

    /** 为当前正常物品补齐缺失索引行；停用墓碑不会被重新启用。 */
    suspend fun reconcileActiveItems(): Int {
        var added = 0
        itemDao.getActiveItems().forEach { item ->
            val existing = indexDao.getByItem(item.id).associateBy { it.imagePath }
            item.imagePaths.forEach { path ->
                if (path !in existing) {
                    indexDao.upsert(
                        ImageAiIndexEntity(
                            imagePath = path,
                            itemId = item.id,
                            status = ImageAnalysisStatus.PENDING.name,
                            requested = true,
                        ),
                    )
                    added++
                }
            }
        }
        return added
    }

    suspend fun loadAnalysis(path: String): VisionAnalysis? {
        val row = indexDao.getByPath(path) ?: return null
        if (!row.requested || row.status != ImageAnalysisStatus.SUCCESS.name) return null
        return row.toDomain()
    }

    suspend fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    companion object {
        const val INDEX_VERSION = 1
    }
}

fun ImageAiIndexEntity.toDomain(): VisionAnalysis = VisionAnalysis(
    objectTags = decodeFields(objectTagsJson),
    attributes = decodeFields(attributesJson),
    visibleText = decodeFields(visibleTextJson),
    description = decodeFields(descriptionJson),
    confidence = confidence,
)

private fun encodeFields(fields: LocalizedVisionFields): String = JSONObject().apply {
    put("zh-Hans", fields.zhHans)
    put("zh-Hant", fields.zhHant)
    put("en", fields.en)
}.toString()

private fun decodeFields(raw: String?): LocalizedVisionFields {
    if (raw.isNullOrBlank()) return LocalizedVisionFields()
    return runCatching {
        val json = JSONObject(raw)
        LocalizedVisionFields(
            zhHans = json.optString("zh-Hans", ""),
            zhHant = json.optString("zh-Hant", ""),
            en = json.optString("en", ""),
        )
    }.getOrDefault(LocalizedVisionFields())
}

/** 模型统一输出为三语言数组；这里编码为可持久化 JSON。 */
fun encodeVisionFields(values: LocalizedVisionFields): String = JSONObject().apply {
    put("zh-Hans", JSONArray(values.zhHans.split(',').map(String::trim).filter(String::isNotEmpty)))
    put("zh-Hant", JSONArray(values.zhHant.split(',').map(String::trim).filter(String::isNotEmpty)))
    put("en", JSONArray(values.en.split(',').map(String::trim).filter(String::isNotEmpty)))
}.toString()

private fun buildSearchText(analysis: VisionAnalysis): String = listOf(
    analysis.objectTags.zhHans,
    analysis.objectTags.zhHant,
    analysis.objectTags.en,
    analysis.attributes.zhHans,
    analysis.attributes.zhHant,
    analysis.attributes.en,
    analysis.visibleText.zhHans,
    analysis.visibleText.zhHant,
    analysis.visibleText.en,
    analysis.description.zhHans,
    analysis.description.zhHant,
    analysis.description.en,
).joinToString(" ").lowercase()