package com.example.smartstorage.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.AppPreferencesRepository
import com.example.smartstorage.data.local.prefs.LlmPreset
import com.example.smartstorage.data.local.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel：管理“AI 智能解析”配置（工作副本 + 保存按钮提交）与外观设置。
 *
 * 进入页面时用 [refresh] 从 Repository 复制一份到工作副本；所有输入只改工作副本；
 * 只有点击“保存配置”才写入 Repository；未保存退出即丢弃（下次进入重新 [refresh]）。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    // ===== AI 智能解析配置（工作副本）=====
    private val _editState = MutableStateFlow(SettingsEditState())
    val editState: StateFlow<SettingsEditState> = _editState.asStateFlow()

    // 使用模式（免费/自定义）：切换时即时持久化，不入工作副本、不计入 hasChanges
    private val _mode = MutableStateFlow(AiConfig.MODE_FREE)
    val mode: StateFlow<String> = _mode.asStateFlow()

    // 当前预设对应的可选模型列表（用于渲染“模型版本”下拉菜单）
    private val _modelList = MutableStateFlow<List<String>>(LlmPreset.DEEPSEEK.models)
    val modelList: StateFlow<List<String>> = _modelList.asStateFlow()

    // 本次会话中各预设的 API Key 工作副本（key = 预设 label，value = 该预设的 Key）
    private val _apiKeys = MutableStateFlow<Map<String, String>>(emptyMap())

    // 本次会话中各预设的模型版本工作副本（key = 预设 label，value = 该预设已选模型）
    private val _modelNames = MutableStateFlow<Map<String, String>>(emptyMap())

    // 进入页面时的原始内容快照（用于判断是否有未保存修改）
    private val _original = MutableStateFlow<SettingsEditState?>(null)

    // 内容字段是否有变更（apiKeyVisible 仅 UI 开关，不计入）
    val hasChanges: StateFlow<Boolean> = combine(_editState, _original) { edit, original ->
        original != null && (
            edit.presetType != original.presetType ||
                edit.baseUrl != original.baseUrl ||
                edit.modelName != original.modelName ||
                edit.apiKey != original.apiKey
            )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 保存成功后的 Snackbar 消息（消费后置空）
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // ===== 外观设置：输入文字颜色 =====
    private val _selectedTextColor = MutableStateFlow(AppPreferencesRepository.DEFAULT_INPUT_TEXT_COLOR)
    val selectedTextColor: StateFlow<Int> = _selectedTextColor.asStateFlow()

    private val _appearanceState = MutableStateFlow<AppearanceSaveState>(AppearanceSaveState.Idle)
    val appearanceState: StateFlow<AppearanceSaveState> = _appearanceState.asStateFlow()

    init {
        _selectedTextColor.value = appPreferencesRepository.getInputTextColor()
        // 首次进入时加载配置（后续每次进入由 MainScreen 调用 refresh）
        refresh()
    }

    /** 从 Repository 读取最新配置并复制到工作副本（丢弃上次未保存的修改）。 */
    fun refresh() {
        viewModelScope.launch {
            val config = settingsRepository.getConfig()
            _mode.value = config.mode
            _apiKeys.value = config.apiKeyMap
            // 把当前预设的有效模型版本并入会话表，切换后再切回能恢复
            _modelNames.value = config.modelNameMap + (config.presetType to config.modelName)
            val snapshot = SettingsEditState(
                presetType = config.presetType,
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                apiKey = config.apiKeyMap[config.presetType] ?: "",
            )
            _editState.value = snapshot
            _original.value = snapshot
            _modelList.value = modelsOf(config.presetType)
        }
    }

    /** 按预设名称查找对应的可选模型列表（查不到返回空列表）。 */
    private fun modelsOf(presetType: String): List<String> =
        LlmPreset.entries.firstOrNull { it.label == presetType }?.models ?: emptyList()

    // ===== AI 配置操作（只改工作副本）=====

    /**
     * 切换使用模式：免费/自定义（即时持久化，免费模式不写任何接口/模型/Key）。
     *
     * 从“自定义”切回“免费”视为明确的“放弃修改”动作：丢弃未保存的自定义配置，
     * 恢复到上次已保存的值，使 hasChanges 归零，不再弹多余的“放弃修改”确认框。
     */
    fun onModeSelect(mode: String) {
        val current = _mode.value
        if (mode == AiConfig.MODE_FREE && current == AiConfig.MODE_CUSTOM) {
            viewModelScope.launch {
                val config = settingsRepository.getConfig()
                val snapshot = SettingsEditState(
                    presetType = config.presetType,
                    baseUrl = config.baseUrl,
                    modelName = config.modelName,
                    apiKey = config.apiKeyMap[config.presetType] ?: "",
                )
                _editState.value = snapshot
                _original.value = snapshot
                _modelList.value = modelsOf(config.presetType)
                _mode.value = mode
                settingsRepository.saveMode(mode)
            }
        } else {
            _mode.value = mode
            viewModelScope.launch {
                settingsRepository.saveMode(mode)
            }
        }
    }

    /** 选择预设：填充 Base URL/模型列表，并恢复该预设已保存的模型版本与 API Key。 */
    fun onPresetSelect(preset: LlmPreset) {
        val current = _editState.value
        // 暂存当前预设未保存的 Key 与模型版本，再加载目标预设已保存的值（无则用默认）
        val updatedKeys = _apiKeys.value + (current.presetType to current.apiKey)
        _apiKeys.value = updatedKeys
        val updatedModels = _modelNames.value + (current.presetType to current.modelName)
        _modelNames.value = updatedModels
        _editState.update {
            it.copy(
                presetType = preset.label,
                baseUrl = preset.baseUrl,
                modelName = updatedModels[preset.label] ?: (preset.models.firstOrNull() ?: ""),
                apiKey = updatedKeys[preset.label] ?: "",
            )
        }
        _modelList.value = preset.models
    }

    fun onBaseUrlChange(value: String) {
        _editState.update { it.copy(baseUrl = value) }
    }

    fun onModelChange(value: String) {
        _editState.update { it.copy(modelName = value) }
    }

    fun onApiKeyChange(value: String) {
        _editState.update { it.copy(apiKey = value) }
    }

    /** 切换 API Key 可见性（纯 UI，不计入变更）。 */
    fun toggleApiKeyVisible() {
        _editState.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }
    }

    /** 保存配置并持久化：仅此方法写入 Repository。 */
    fun saveAiConfig() {
        val state = _editState.value
        if (state.baseUrl.isBlank() || state.modelName.isBlank() || state.apiKey.isBlank()) {
            _saveMessage.value = "请填写完整的接口地址、模型版本和 API Key"
            return
        }
        viewModelScope.launch {
            // 把当前预设的 Key 与模型版本合并进会话表，随配置一并保存到对应预设名下
            val updatedKeys = _apiKeys.value + (state.presetType to state.apiKey.trim())
            _apiKeys.value = updatedKeys
            val updatedModels = _modelNames.value + (state.presetType to state.modelName.trim())
            _modelNames.value = updatedModels
            settingsRepository.saveConfig(
                AiConfig(
                    presetType = state.presetType,
                    baseUrl = state.baseUrl.trim(),
                    modelNameMap = updatedModels,
                    apiKeyMap = updatedKeys,
                    mode = _mode.value,
                ),
            )
            // 保存成功后同步原始快照：此后退出不再提示
            _original.value = _editState.value
            _saveMessage.value = "AI 智能解析配置已保存 ✅"
        }
    }

    /** 消费 Snackbar 消息。 */
    fun consumeSaveMessage() {
        _saveMessage.value = null
    }

    // ===== 首页搜索小贴士 =====

    /** 首页“搜索小贴士”弹窗是否启用。 */
    fun isSearchTipsEnabled(): Boolean = appPreferencesRepository.getSearchTipsEnabled()

    /** 设置首页“搜索小贴士”弹窗是否启用。 */
    fun setSearchTipsEnabled(enabled: Boolean) {
        appPreferencesRepository.setSearchTipsEnabled(enabled)
    }

    // ===== 外观设置操作 =====

    /** 选择输入文字颜色：立即保存并生效。 */
    fun selectTextColor(argb: Int) {
        _selectedTextColor.value = argb
        appPreferencesRepository.setInputTextColor(argb)
        _appearanceState.value = AppearanceSaveState.Saved
    }
}

/** AI 智能解析配置编辑工作副本（UI 唯一绑定源）。 */
data class SettingsEditState(
    /** 当前预设名称 */
    val presetType: String = LlmPreset.DEEPSEEK.label,

    /** 接口地址 */
    val baseUrl: String = LlmPreset.DEEPSEEK.baseUrl,

    /** 模型名称 */
    val modelName: String = LlmPreset.DEEPSEEK.model,

    /** API Key */
    val apiKey: String = "",

    /** API Key 是否明文显示（仅 UI，不计入内容变更） */
    val apiKeyVisible: Boolean = false,
)

/** 可选的输入文字颜色。 */
data class TextColorOption(
    val name: String,
    val argb: Int,
)

/** 外观设置保存状态。 */
sealed interface AppearanceSaveState {
    /** 初始空闲 */
    data object Idle : AppearanceSaveState

    /** 已保存 */
    data object Saved : AppearanceSaveState
}