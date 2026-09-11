package com.example.smartstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartstorage.data.local.entity.ImageAiIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageAiIndexDao {
    @Query("SELECT * FROM image_ai_indices ORDER BY analyzed_at DESC, image_path ASC")
    fun observeAll(): Flow<List<ImageAiIndexEntity>>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'SUCCESS'")
    suspend fun getAll(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'SUCCESS'")
    fun observeSuccessful(): Flow<List<ImageAiIndexEntity>>

    @Query("SELECT * FROM image_ai_indices WHERE item_id = :itemId")
    suspend fun getByItem(itemId: Long): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE image_path = :path LIMIT 1")
    suspend fun getByPath(path: String): ImageAiIndexEntity?

    @Query("SELECT * FROM image_ai_indices WHERE status IN ('PENDING', 'OUTDATED') ORDER BY image_path ASC")
    suspend fun getQueued(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'FAILED' ORDER BY image_path ASC")
    suspend fun getFailed(): List<ImageAiIndexEntity>

    @Query("SELECT * FROM image_ai_indices WHERE status = 'PROCESSING'")
    suspend fun getProcessing(): List<ImageAiIndexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ImageAiIndexEntity)

    @Query("DELETE FROM image_ai_indices WHERE image_path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM image_ai_indices WHERE item_id = :itemId")
    suspend fun deleteByItem(itemId: Long)

    @Query("DELETE FROM image_ai_indices")
    suspend fun deleteAll()

    @Query("UPDATE image_ai_indices SET status = 'PROCESSING', error_kind = NULL WHERE image_path = :path")
    suspend fun markProcessing(path: String)

    @Query("UPDATE image_ai_indices SET status = 'PENDING' WHERE status = 'PROCESSING'")
    suspend fun restoreProcessing()

    @Query("UPDATE image_ai_indices SET status = 'PENDING' WHERE status = 'FAILED'")
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
        """,
    )
    suspend fun markSuccess(
        path: String,
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
    )

    @Query(
        """
        UPDATE image_ai_indices
        SET content_hash = :hash,
            status = 'FAILED',
            error_kind = :errorKind,
            analyzed_at = :analyzedAt
        WHERE image_path = :path
        """,
    )
    suspend fun markFailed(path: String, hash: String?, errorKind: String, analyzedAt: Long)

    @Query("UPDATE image_ai_indices SET status = 'OUTDATED' WHERE image_path = :path")
    suspend fun markOutdated(path: String)
}
