package com.example.smartstorage.di

import com.example.smartstorage.data.remote.llm.LlmTransport
import com.example.smartstorage.data.remote.llm.OkHttpLlmTransport
import com.example.smartstorage.data.remote.vision.OkHttpVisionTransport
import com.example.smartstorage.data.remote.vision.VisionTransport
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

    /**
     * 提供全局 OkHttpClient（仅大模型 HTTP 请求使用）。
     *
     * - 连接超时 7 秒：网络不可达时快速失败；
     * - 读取超时 25 秒：免费内置 7B 模型推理偏慢，避免正常慢响应被误判成“解析超时”，
     *   真正的超时仍会走 UI 的降级/配置出口，而不是“只延长等待或只改文案”；
     * - 写超时 7 秒：请求体很小，7 秒足够。
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(7, TimeUnit.SECONDS)
        .build()

    /** 大模型 HTTP 传输实现：可替换为测试假实现做故障注入。 */
    @Provides
    @Singleton
    fun provideLlmTransport(client: OkHttpClient): LlmTransport = OkHttpLlmTransport(client)

    /** OpenAI 兼容视觉传输实现。 */
    @Provides
    @Singleton
    fun provideVisionTransport(client: OkHttpClient): VisionTransport = OkHttpVisionTransport(client)
}