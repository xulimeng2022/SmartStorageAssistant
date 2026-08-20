package com.example.smartstorage.domain.repository

import com.example.smartstorage.domain.model.Item
import kotlinx.coroutines.flow.Flow

/**
 * 物品仓库接口：对上层隐藏数据来源细节。
 */
interface ItemRepository {

    /** 观察全部物品。 */
    fun observeItems(): Flow<List<Item>>

    /** 按 ID 观察单个物品。 */
    fun observeItemById(id: Long): Flow<Item?>

    /** 观察回收站物品（已软删除），按删除时间倒序排列。 */
    fun observeTrash(): Flow<List<Item>>

    /** 按名称或存放地点模糊搜索物品（查询为空时等价于观察全部）。 */
    fun searchItems(query: String): Flow<List<Item>>

    /** 按名称/地点/描述三字段精确筛选（空字段不限制，AND 组合）。 */
    fun searchByFields(name: String, location: String, description: String): Flow<List<Item>>

    /** 按名称精确查询（忽略大小写）；查不到返回 null。 */
    suspend fun getItemByName(name: String): Item?

    /** 新增物品，返回新记录 ID。 */
    suspend fun addItem(item: Item): Long

    /** 更新物品。 */
    suspend fun updateItem(item: Item)

    /** 软删除：将物品移入回收站（不删除数据与照片文件）。 */
    suspend fun deleteItem(item: Item)

    /** 恢复：将回收站物品移回主列表。 */
    suspend fun restoreItem(item: Item)

    /** 永久删除：物理删除记录并同步删除关联的照片文件。 */
    suspend fun permanentDeleteItem(item: Item)

    /** 清空回收站：批量物理删除所有已删除记录及其照片文件。 */
    suspend fun emptyTrash()
}