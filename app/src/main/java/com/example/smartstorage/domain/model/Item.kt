package com.example.smartstorage.domain.model

/**
 * 物品领域模型（与数据库实体解耦）。
 *
 * @property id 唯一 ID（新增时为 0，由数据库自增生成）
 * @property name 物品名称
 * @property location 存放地点
 * @property description 详细描述
 * @property imagePaths 照片附件在本地文件系统的绝对路径列表（可为空）
 * @property createdAt 创建时间（毫秒时间戳）
 * @property updatedAt 最后修改时间（毫秒时间戳）
 * @property deletedAt 删除时间（毫秒时间戳）；null 表示正常，非 null 表示在回收站
 */
data class Item(
    val id: Long = 0L,
    val name: String,
    val location: String = "",
    val description: String = "",
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
) {
    /** 第一张照片路径（兼容单图调用点；无照片返回 null）。 */
    val imagePath: String? get() = imagePaths.firstOrNull()
}