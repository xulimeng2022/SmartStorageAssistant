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

    suspend fun getQueued(): List<ImageAiIndexEntity> = indexDao.getQueued()

    suspend fun getFailed(): List<ImageAiIndexEntity> = indexDao.getFailed()

    suspend fun markProcessing(path: String) = indexDao.markProcessing(path)

    suspend fun restoreProcessing() = indexDao.restoreProcessing()

    suspend fun retryFailed() = indexDao.retryFailed()

    suspend fun clearAll() = indexDao.deleteAll()

    suspend fun deleteItem(itemId: Long) = indexDao.deleteByItem(itemId)

    suspend fun markFailed(path: String, hash: String?, errorKind: String) =
        indexDao.markFailed(path, hash, errorKind, System.currentTimeMillis())

    suspend fun saveSuccess(
        path: String,
        hash: String,
        analysis: VisionAnalysis,
        provider: String,
        model: String,
    ) {
        indexDao.markSuccess(
            path = path,
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
        )
    }

    /** 保存/编辑物品后同步索引行；未变化路径保留已有成功结果。 */
    suspend fun syncItemImages(itemId: Long, paths: List<String>) {
        val existing = indexDao.getByItem(itemId).associateBy { it.imagePath }
        paths.forEach { path ->
            val row = existing[path]
            if (row == null) {
                val source = indexDao.getByPath(path)
                if (source != null && source.itemId != itemId) {
                    indexDao.deleteByPath(path)
                }
                indexDao.upsert(
                    ImageAiIndexEntity(
                        imagePath = path,
                        itemId = itemId,
                        status = ImageAnalysisStatus.PENDING.name,
                    ),
                )
            }
        }
        existing.keys.filterNot { it in paths }.forEach { indexDao.deleteByPath(it) }
    }

    /** 为当前正常物品补齐缺失索引行，不触碰回收站。 */
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
                        ),
                    )
                    added++
                }
            }
        }
        return added
    }

    suspend fun loadAnalysis(path: String): VisionAnalysis? =
        indexDao.getByPath(path)?.takeIf { it.status == ImageAnalysisStatus.SUCCESS.name }?.toDomain()

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
