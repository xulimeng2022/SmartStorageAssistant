package com.example.smartstorage.data.repository

import com.example.smartstorage.data.local.dao.ItemDao
import com.example.smartstorage.data.local.image.ImageStorage
import com.example.smartstorage.data.mapper.toDomain
import com.example.smartstorage.data.mapper.toEntity
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 物品仓库实现：基于 Room 数据源。
 */
@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val imageStorage: ImageStorage,
) : ItemRepository {

    override fun observeItems(): Flow<List<Item>> =
        itemDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActiveCount(): Flow<Int> = itemDao.observeActiveCount()

    override fun observeItemById(id: Long): Flow<Item?> =
        itemDao.observeById(id).map { it?.toDomain() }

    override fun observeTrash(): Flow<List<Item>> =
        itemDao.observeTrash().map { list -> list.map { it.toDomain() } }

    override fun searchItems(query: String): Flow<List<Item>> {
        val keyword = query.trim()
        // 空关键字直接返回全部，避免 LIKE '%%' 的无效查询
        if (keyword.isEmpty()) return observeItems()
        return itemDao.searchByNameOrLocation(keyword)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun searchByFields(name: String, location: String, description: String): Flow<List<Item>> =
        itemDao.searchByFields(name, location, description)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getItemByName(name: String): Item? =
        itemDao.getItemByName(name)?.toDomain()

    override suspend fun addItem(item: Item): Long =
        itemDao.insert(item.toEntity())

    override suspend fun updateItem(item: Item) {
        itemDao.update(item.toEntity())
    }

    override suspend fun deleteItem(item: Item) {
        // 软删除：仅写入删除时间戳，保留数据与照片文件（可在回收站恢复）
        itemDao.softDelete(item.id, System.currentTimeMillis())
    }

    override suspend fun restoreItem(item: Item) {
        itemDao.restore(item.id)
    }

    override suspend fun permanentDeleteItem(item: Item) {
        // 永久删除：先清理全部照片文件，再物理删除记录
        item.imagePaths.forEach { imageStorage.deleteImage(it) }
        itemDao.permanentDelete(item.id)
    }

    override suspend fun emptyTrash() {
        // 清空回收站：先清理全部照片文件，再批量物理删除记录
        itemDao.getTrashedItems().forEach { entity ->
            entity.imagePaths.forEach { imageStorage.deleteImage(it) }
        }
        itemDao.permanentDeleteAll()
    }
}