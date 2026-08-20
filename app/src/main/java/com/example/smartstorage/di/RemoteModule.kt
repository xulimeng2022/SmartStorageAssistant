package com.example.smartstorage.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络相关依赖模块。
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    /** 提供全局 OkHttpClient（仅大模型 HTTP 请求使用；7 秒超时用于免费模式超时引导）。 */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .writeTimeout(7, TimeUnit.SECONDS)
        .build()
}