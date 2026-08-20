package com.example.smartstorage.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartstorage.data.local.converters.Converters
import com.example.smartstorage.data.local.dao.ItemDao
import com.example.smartstorage.data.local.entity.ItemEntity

/**
 * 应用本地数据库。
 */
@Database(
    entities = [ItemEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /** 物品表 DAO。 */
    abstract fun itemDao(): ItemDao

    companion object {
        /**
         * v1 → v2：新增照片附件列 image_path。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN image_path TEXT")
            }
        }

        /**
         * v2 → v3：新增回收站软删除列 deleted_at（默认 null 表示正常）。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN deleted_at INTEGER")
            }
        }

        /**
         * v3 → v4：单图 image_path 迁移为 JSON 数组（多图）。旧值包装为 ["旧路径"]。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE items SET image_path = '[\"' || image_path || '\"]' " +
                        "WHERE image_path IS NOT NULL AND image_path NOT LIKE '[%'"
                )
            }
        }
    }
}