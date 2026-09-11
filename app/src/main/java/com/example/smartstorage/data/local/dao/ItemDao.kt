package com.example.smartstorage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartstorage.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * 物品表数据访问对象。
 */
@Dao
interface ItemDao {

    /** 观察全部正常物品（不含回收站），按最后修改时间倒序排列。 */
    @Query("SELECT * FROM items WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    /** 观察正常物品总数（不含回收站），用于首页区分“从未添加”与“搜索无结果”。 */
    @Query("SELECT COUNT(*) FROM items WHERE deleted_at IS NULL")
    fun observeActiveCount(): Flow<Int>

    /** 按 ID 观察单个物品。 */
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ItemEntity?>

    /**
     * 按名称精确查询（忽略大小写，LOWER 做 ASCII 大小写折叠），
     * 多条同名时取最近更新的那条；查不到返回 null。
     */
    @Query(
        "SELECT * FROM items WHERE LOWER(name) = LOWER(:name) AND deleted_at IS NULL " +
            "ORDER BY updated_at DESC LIMIT 1"
    )
    suspend fun getItemByName(name: String): ItemEntity?

        /**
     * 逐字模糊搜索：pattern 由调用方生成（去掉空白后逐字用 % 连接，如“红色 口红”/“红色口红”→ %红%色%口%红%）；
     * 命中名称 / 存放地点 / 备注任一字段即返回；同时保留“反向包含”（关键词包含存储值，如“车里面”命中“车里”）；
     * 大小写：SQLite LIKE 对 ASCII 默认忽略大小写；结果按最后修改时间倒序排列。
     */
    @Query(
        """
        SELECT * FROM items
        WHERE deleted_at IS NULL
          AND (name LIKE :pattern ESCAPE '\'
           OR location LIKE :pattern ESCAPE '\'
           OR description LIKE :pattern ESCAPE '\'
           OR (name <> '' AND :query LIKE '%' || name || '%')
           OR (location <> '' AND :query LIKE '%' || location || '%')
           OR (description <> '' AND :query LIKE '%' || description || '%'))
        ORDER BY updated_at DESC
        """
    )
    fun searchFuzzy(pattern: String, query: String): Flow<List<ItemEntity>>
    /**
     * 按名称/地点/描述三字段精确筛选（空字段不限制，AND 组合，不含回收站）。
     * 每字段用双向包含模糊匹配：存储值包含关键词，或关键词包含存储值（如“车里”与“车里面”互相命中）。
     */
    @Query(
        """
        SELECT * FROM items
        WHERE deleted_at IS NULL
          AND (:name = ''
               OR name LIKE '%' || :name || '%'
               OR (name <> '' AND :name LIKE '%' || name || '%'))
          AND (:location = ''
               OR location LIKE '%' || :location || '%'
               OR (location <> '' AND :location LIKE '%' || location || '%'))
          AND (:description = ''
               OR description LIKE '%' || :description || '%'
               OR (description <> '' AND :description LIKE '%' || description || '%'))
        ORDER BY updated_at DESC
        """
    )
    fun searchByFields(name: String, location: String, description: String): Flow<List<ItemEntity>>

    /** 新增物品，返回新记录的自增 ID。 */
    @Insert
    suspend fun insert(item: ItemEntity): Long

    /** 更新物品。 */
    @Update
    suspend fun update(item: ItemEntity)

    /** 物理删除物品（仅供回收站永久删除使用）。 */
    @Delete
    suspend fun delete(item: ItemEntity)

    /** 观察回收站物品（已软删除），按删除时间倒序排列。 */
    @Query("SELECT * FROM items WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeTrash(): Flow<List<ItemEntity>>

    /** 一次性查询回收站全部物品（用于清空时清理图片文件）。 */
    @Query("SELECT * FROM items WHERE deleted_at IS NOT NULL")
    suspend fun getTrashedItems(): List<ItemEntity>

    /** 软删除：将物品移入回收站（仅写入删除时间戳，不删除数据与图片文件）。 */
    @Query("UPDATE items SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    /** 恢复：将回收站物品移回主列表。 */
    @Query("UPDATE items SET deleted_at = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    /** 物理删除单条记录（永久删除，调用方需自行清理图片文件）。 */
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun permanentDelete(id: Long)

    /** 清空回收站：物理删除所有已删除记录（调用方需先清理图片文件）。 */
    @Query("DELETE FROM items WHERE deleted_at IS NOT NULL")
    suspend fun permanentDeleteAll()

    /** 查询全部物品（含回收站），用于数据备份导出 */
    @Query("SELECT * FROM items")
    suspend fun getAllItems(): List<ItemEntity>

    /** 查询全部正常物品（含图片路径），用于同步视觉索引。 */
    @Query("SELECT * FROM items WHERE deleted_at IS NULL")
    suspend fun getActiveItems(): List<ItemEntity>

    /** 物理删除全部记录（仅供覆盖导入前清空使用） */
    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    /** 按名称精确查重（忽略大小写，含回收站），用于合并导入去重 */
    @Query("SELECT * FROM items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): ItemEntity?
}
