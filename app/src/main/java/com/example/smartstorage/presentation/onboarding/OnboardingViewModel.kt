package com.example.smartstorage.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.prefs.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 引导页 ViewModel：读取首次启动标志决定是否展示引导页，完成后持久化并关闭。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    // 是否显示引导页：null = 读取中（短暂空白，避免非首次用户闪一下引导页）；true = 首次启动；false = 非首次
    private val _showOnboarding = MutableStateFlow<Boolean?>(null)
    val showOnboarding: StateFlow<Boolean?> = _showOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            // 读取持久化的首次启动标志，决定是否展示引导页
            _showOnboarding.value = onboardingRepository.isFirstLaunch.first()
        }
    }

    /** 完成引导：写入非首次启动标志，并关闭引导页。 */
    fun onFinished() {
        viewModelScope.launch {
            onboardingRepository.completeOnboarding()
            _showOnboarding.value = false
        }
    }
}