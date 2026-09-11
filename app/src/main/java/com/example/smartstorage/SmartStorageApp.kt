package com.example.smartstorage

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.smartstorage.data.local.prefs.AppLanguage
import com.example.smartstorage.data.local.prefs.ImageIndexJobState
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import com.example.smartstorage.data.remote.vision.ImageIndexingCoordinator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：启用 Hilt 依赖注入。
 */
@HiltAndroidApp
class SmartStorageApp : Application(), Configuration.Provider {

    @javax.inject.Inject
    lateinit var workerFactory: HiltWorkerFactory

    @javax.inject.Inject
    lateinit var understandingRepository: ImageUnderstandingRepository

    @javax.inject.Inject
    lateinit var indexingCoordinator: ImageIndexingCoordinator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val state = understandingRepository.state.value
            if (
                state.enabled &&
                state.capability == VisionCapabilityStatus.SUPPORTED &&
                state.jobState == ImageIndexJobState.ACTIVE
            ) {
                indexingCoordinator.resume()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** 应用级 Context 也跟随用户选择的语言，供 ViewModel 的 Toast/消息使用。 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }
}