package com.example.smartstorage.data.mapper

import com.example.smartstorage.data.local.entity.ItemEntity
import com.example.smartstorage.domain.model.Item

/** 领域模型转数据库实体。 */
fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    name = name,
    location = location,
    description = description,
    imagePaths = imagePaths,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** 数据库实体转领域模型。 */
fun ItemEntity.toDomain(): Item = Item(
    id = id,
    name = name,
    location = location,
    description = description,
    imagePaths = imagePaths,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)