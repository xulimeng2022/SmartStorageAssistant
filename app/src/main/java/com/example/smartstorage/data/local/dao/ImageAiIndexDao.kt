package com.example.smartstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageAiIndexDao {
    /** 观察有效索引；requested=0 的停用墓碑不参与进度和搜索。 */
    @Query("SELECT * FROM image_ai_indices WHERE requested = 1 ORDER BY analyzed_at DESC, image_path ASC")
    fun observeAll(): Flow<List<ImageAiIndexEntity>>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'SUCCESS' AND requested = 1")
    suspend fun getAll(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'SUCCESS' AND requested = 1")
    fun observeSuccessful(): Flow<List<ImageAiIndexEntity>>

    /** 详情/编辑页观察单件物品的全部行，包含停用墓碑以展示“未创建”状态。 */
    @Query("SELECT * FROM image_ai_indices WHERE item_id = :itemId ORDER BY image_path ASC")
    fun observeByItem(itemId: Long): Flow<List<ImageAiIndexEntity>>

    @Query("SELECT * FROM image_ai_indices WHERE item_id = :itemId ORDER BY image_path ASC")
    suspend fun getByItem(itemId: Long): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE image_path = :path LIMIT 1")
    suspend fun getByPath(path: String): ImageAiIndexEntity?

    @Query(
        "SELECT index_row.* FROM image_ai_indices AS index_row " +
            "INNER JOIN items AS item ON item.id = index_row.item_id " +
            "WHERE index_row.requested = 1 AND index_row.status IN ('PENDING', 'OUTDATED') " +
            "AND item.deleted_at IS NULL ORDER BY index_row.image_path ASC"
    )
    suspend fun getQueued(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE requested = 1 AND status = 'FAILED' ORDER BY image_path ASC")
    suspend fun getFailed(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE requested = 1 AND status = 'PROCESSING'")
    suspend fun getProcessing(): List<ImageAiIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ImageAiIndexEntity)

    @Query("DELETE FROM image_ai_indices WHERE image_path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM image_ai_indices WHERE item_id = :itemId")
    suspend fun deleteByItem(itemId: Long)

    @Query("DELETE FROM image_ai_indices")
    suspend fun deleteAll()

    @Query(
        "UPDATE image_ai_indices SET requested = 1, status = 'PENDING', error_kind = NULL, " +
            "generation = generation + 1 WHERE image_path = :path AND item_id = :itemId"
    )
    suspend fun requestExisting(path: String, itemId: Long): Int

    @Query(
        "UPDATE image_ai_indices SET requested = 0, status = 'OUTDATED', error_kind = NULL, " +
            "generation = generation + 1 WHERE image_path = :path"
    )
    suspend fun disableByPath(path: String): Int

    @Query(
        "UPDATE image_ai_indices SET status = 'PROCESSING', error_kind = NULL " +
            "WHERE image_path = :path AND generation = :generation AND requested = 1 " +
            "AND status IN ('PENDING', 'OUTDATED')"
    )
    suspend fun markProcessing(path: String, generation: Long): Int

    @Query("UPDATE image_ai_indices SET status = 'PENDING' WHERE status = 'PROCESSING' AND requested = 1")
    suspend fun restoreProcessing()

    @Query("UPDATE image_ai_indices SET status = 'PENDING' WHERE status = 'FAILED' AND requested = 1")
    suspend fun retryFailed()

    @Query(
        """
        UPDATE image_ai_indices
        SET content_hash = :hash,
            object_tags_json = :objects,
            attributes_json = :attributes,
            visible_text_json = :visibleText,
            description_json = :description,
            search_text = :searchText,
            confidence = :confidence,
            status = 'SUCCESS',
            analysis_provider = :provider,
            analysis_model = :model,
            index_version = :indexVersion,
            analyzed_at = :analyzedAt,
            error_kind = NULL
        WHERE image_path = :path
          AND generation = :generation
          AND requested = 1
          AND status = 'PROCESSING'
        """
    )
    suspend fun markSuccess(
        path: String,
        generation: Long,
        hash: String,
        objects: String,
        attributes: String,
        visibleText: String,
        description: String,
        searchText: String,
        confidence: Double,
        provider: String,
        model: String,
        indexVersion: Int,
        analyzedAt: Long,
    ): Int

    @Query(
        """
        UPDATE image_ai_indices
        SET content_hash = :hash,
            status = 'FAILED',
            error_kind = :errorKind,
            analyzed_at = :analyzedAt
        WHERE image_path = :path
          AND generation = :generation
          AND requested = 1
          AND status = 'PROCESSING'
        """
    )
    suspend fun markFailed(
        path: String,
        generation: Long,
        hash: String?,
        errorKind: String,
        analyzedAt: Long,
    ): Int

    @Query(
        "UPDATE image_ai_indices SET status = 'OUTDATED' " +
            "WHERE image_path = :path AND generation = :generation AND requested = 1"
    )
    suspend fun markOutdated(path: String, generation: Long): Int
}
