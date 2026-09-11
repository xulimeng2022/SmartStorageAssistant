package com.example.smartstorage.di

import android.content.Context
import androidx.room.Room
import com.example.smartstorage.data.local.AppDatabase
import com.example.smartstorage.data.local.dao.ImageAiIndexDao
import com.example.smartstorage.data.local.dao.ItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库相关依赖提供模块。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** 提供应用数据库实例。 */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_storage.db",
        )
            // 使用显式迁移脚本（v1→v2 增加 image_path 列；v2→v3 增加 deleted_at 列；v3→v4 单图转多图 JSON），迁移失败时兜底破坏性重建
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigration()
            .build()

    /** 提供物品表 DAO。 */
    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    /** 提供图片视觉索引 DAO。 */
    @Provides
    fun provideImageAiIndexDao(database: AppDatabase): ImageAiIndexDao = database.imageAiIndexDao()
}