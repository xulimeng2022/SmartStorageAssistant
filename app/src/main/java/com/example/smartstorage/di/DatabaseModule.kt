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

/** 数据库相关依赖提供模块。 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** 提供应用数据库实例；只使用显式迁移，禁止升级失败时破坏性重建。 */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_storage.db",
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
            )
            .build()

    /** 提供物品表 DAO。 */
    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    /** 提供图片视觉索引 DAO。 */
    @Provides
    fun provideImageAiIndexDao(database: AppDatabase): ImageAiIndexDao = database.imageAiIndexDao()
}