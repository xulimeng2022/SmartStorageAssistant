package com.example.smartstorage.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 物品清单表（Room 实体）。
 *
 * @property id 唯一 ID（自增主键）
 * @property name 物品名称
 * @property location 存放地点
 * @property description 详细描述
 * @property imagePaths 照片附件在本地文件系统的绝对路径列表（可为空）
 * @property createdAt 创建时间（毫秒时间戳）
 * @property updatedAt 最后修改时间（毫秒时间戳）
 * @property deletedAt 删除时间（毫秒时间戳）；null 表示正常，非 null 表示在回收站
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "image_path")
    val imagePaths: List<String> = emptyList(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)