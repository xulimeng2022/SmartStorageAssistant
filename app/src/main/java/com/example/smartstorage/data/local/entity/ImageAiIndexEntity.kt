package com.example.smartstorage.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 单张图片的本地 AI 视觉索引；图片路径是稳定主键。 */
@Entity(
    tableName = "image_ai_indices",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("item_id"),
        Index("status"),
    ],
)
data class ImageAiIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "image_path")
    val imagePath: String,

    @ColumnInfo(name = "item_id")
    val itemId: Long,

    @ColumnInfo(name = "content_hash")
    val contentHash: String? = null,

    @ColumnInfo(name = "object_tags_json")
    val objectTagsJson: String = "{}",

    @ColumnInfo(name = "attributes_json")
    val attributesJson: String = "{}",

    @ColumnInfo(name = "visible_text_json")
    val visibleTextJson: String = "{}",

    @ColumnInfo(name = "description_json")
    val descriptionJson: String = "{}",

    @ColumnInfo(name = "search_text")
    val searchText: String = "",

    @ColumnInfo(name = "confidence")
    val confidence: Double = 0.0,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "analysis_provider")
    val analysisProvider: String? = null,

    @ColumnInfo(name = "analysis_model")
    val analysisModel: String? = null,

    @ColumnInfo(name = "index_version")
    val indexVersion: Int = 1,

    @ColumnInfo(name = "analyzed_at")
    val analyzedAt: Long? = null,

    @ColumnInfo(name = "error_kind")
    val errorKind: String? = null,
)
