package com.example.smartstorage.presentation.settings

import com.example.smartstorage.R
import com.example.smartstorage.presentation.common.UiMessage
import com.example.smartstorage.presentation.common.toFailureUiMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.smartstorage.data.local.backup.BackupInfo
import com.example.smartstorage.data.local.backup.BackupRepository
import com.example.smartstorage.data.local.backup.ImportMode
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.AppPreferencesRepository
import com.example.smartstorage.data.local.prefs.LlmPreset
import com.example.smartstorage.data.local.prefs.ImageIndexJobState
import com.example.smartstorage.data.local.prefs.ImageUnderstandingRepository
import com.example.smartstorage.data.local.prefs.ImageUnderstandingState
import com.example.smartstorage.data.local.prefs.SettingsRepository
import com.example.smartstorage.data.local.prefs.VisionCapabilityStatus
import com.example.smartstorage.data.remote.vision.ImageIndexingCoordinator
import com.example.smartstorage.data.remote.vision.VisionAnalyzer
import com.example.smartstorage.data.remote.vision.VisionProbeResult
import com.example.smartstorage.data.repository.ImageIndexProgress
import com.example.smartstorage.data.repository.ImageAiIndexRepository
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.ThemeMode
import com.example.smartstorage.data.local.prefs.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 设置页 ViewModel：管理「AI 智能解析」配置（工作副本 + 保存按钮提交）、
 * 外观设置（主题模式 / 全局文字颜色，即时保存）与搜索小贴士开关。
 *
 * 进入页面时用 [refresh] 从 Repository 复制 AI 配置到工作副本；所有输入只改工作副本；
 * 只有点击「保存配置」才写入 Repository；未保存退出即丢弃（下次进入重置 [refresh]）。
 * 主题模式与文字颜色为实时流订阅，修改即持久化、全局立即生效，不纳入 hasChanges。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val themeRepository: ThemeRepository,
    private val backupRepository: BackupRepository,
    private val imageUnderstandingRepository: ImageUnderstandingRepository,
    private val imageIndexingCoordinator: ImageIndexingCoordinator,
    private val imageAiIndexRepository: ImageAiIndexRepository,
    private val visionAnalyzer: VisionAnalyzer,
) : ViewModel() {

    // ===== AI 智能解析配置（工作副本）=====
    private val _editState = MutableStateFlow(SettingsEditState())
    val editState: StateFlow<SettingsEditState> = _editState.asStateFlow()

    // 使用模式（免费 / 自定义）：切换时即时持久化，不入工作副本、不计入 hasChanges
    private val _mode = MutableStateFlow(AiConfig.MODE_FREE)
    val mode: StateFlow<String> = _mode.asStateFlow()

    // 当前预设对应的可选模型列表（用于渲染「模型版本」下拉菜单）
    private val _modelList = MutableStateFlow<List<String>>(LlmPreset.DEEPSEEK.models)
    val modelList: StateFlow<List<String>> = _modelList.asStateFlow()

    // 本次会话中各预设的 API Key 工作副本（key = 预设 label，value = 该预设的 Key）
    private val _apiKeys = MutableStateFlow<Map<String, String>>(emptyMap())

    // 本次会话中各预设的模型版本工作副本（key = 预设 label，value = 该预设已选模型）
    private val _modelNames = MutableStateFlow<Map<String, String>>(emptyMap())

    // 进入页面时的原始内容快照（用于判断是否有未保存修改）
    private val _original = MutableStateFlow<SettingsEditState?>(null)

    // 内容字段是否有变更（apiKeyVisible 为 UI 开关，不计入）
    val hasChanges: StateFlow<Boolean> = combine(_editState, _original) { edit, original ->
        original != null && (
            edit.presetType != original.presetType ||
                edit.baseUrl != original.baseUrl ||
                edit.modelName != original.modelName ||
                edit.apiKey != original.apiKey
            )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 保存成功后的 Snackbar 消息（消费后置空）；只存资源 ID + 参数，由设置页按当前语言解析
    private val _saveMessage = MutableStateFlow<UiMessage?>(null)
    val saveMessage: StateFlow<UiMessage?> = _saveMessage.asStateFlow()

    // ===== 外观设置：主题模式（跟随系统 / 浅色 / 深色）=====
    private val _themeMode = MutableStateFlow(ThemeMode.FOLLOW_SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // ===== 外观设置：全局文字颜色配置 =====
    private val _textColorConfig = MutableStateFlow(TextColorConfig())
    val textColorConfig: StateFlow<TextColorConfig> = _textColorConfig.asStateFlow()

    init {
        // 订阅主题模式：与 MainActivity 共用同一 DataStore 流，切换后全局立即一致
        viewModelScope.launch {
            themeRepository.themeMode.collect { _themeMode.value = it }
        }
        // 订阅全局文字颜色：设置页修改后立即反映到当前抽屉，其他页面同时生效
        viewModelScope.launch {
            themeRepository.textColorConfig.collect { _textColorConfig.value = it }
        }
        // 首次进入时加载 AI 配置（后续每次进入由 MainScreen 调用 refresh）
        refresh()
    }

    /** 从 Repository 读取最新 AI 配置并复制到工作副本（丢弃上次未保存的修改）*/
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

    /** 按预设名称查找对应的可选模型列表（查不到返回空列表）*/
    private fun modelsOf(presetType: String): List<String> =
        LlmPreset.entries.firstOrNull { it.label == presetType }?.models ?: emptyList()

    val visionState: StateFlow<ImageUnderstandingState> = imageUnderstandingRepository.state
    val visionProgress: StateFlow<ImageIndexProgress> = imageAiIndexRepository.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImageIndexProgress())

    private val _showVisionPrivacyDialog = MutableStateFlow(false)
    val showVisionPrivacyDialog: StateFlow<Boolean> = _showVisionPrivacyDialog.asStateFlow()

    private val _visionActionState = MutableStateFlow<VisionActionState>(VisionActionState.Idle)
    val visionActionState: StateFlow<VisionActionState> = _visionActionState.asStateFlow()

    fun requestVisionToggle(enabled: Boolean) {
        if (enabled) {
            _showVisionPrivacyDialog.value = true
        } else {
            viewModelScope.launch {
                imageUnderstandingRepository.setEnabled(false)
                imageIndexingCoordinator.cancel()
            }
        }
    }

    fun dismissVisionPrivacyDialog() {
        _showVisionPrivacyDialog.value = false
    }

    fun confirmEnableVision() {
        _showVisionPrivacyDialog.value = false
        _visionActionState.value = VisionActionState.Probing
        viewModelScope.launch {
            val config = visionAnalyzer.currentConfig()
            when (val result = visionAnalyzer.probe(config)) {
                VisionProbeResult.SUPPORTED -> {
                    imageUnderstandingRepository.saveCapability(config.fingerprint, VisionCapabilityStatus.SUPPORTED)
                    imageUnderstandingRepository.setEnabled(true)
                    _visionActionState.value = VisionActionState.Idle
                }
                else -> {
                    // 失败只保留本次会话提示，不持久化负缓存：用户下次开启仍会做一次真实探测
                    imageUnderstandingRepository.invalidateCapability()
                    imageUnderstandingRepository.setEnabled(false)
                    val message = result.toFailureUiMessage()
                    _visionActionState.value = if (message == null) {
                        VisionActionState.Idle
                    } else {
                        VisionActionState.Failed(message)
                    }
                }
            }
        }
    }

    fun consumeVisionActionState() {
        _visionActionState.value = VisionActionState.Idle
    }

    fun startHistoryIndexing() {
        viewModelScope.launch { imageIndexingCoordinator.startHistory() }
    }

    fun pauseHistoryIndexing() {
        viewModelScope.launch { imageIndexingCoordinator.pause() }
    }

    fun resumeHistoryIndexing() {
        viewModelScope.launch { imageIndexingCoordinator.resume() }
    }

    fun cancelHistoryIndexing() {
        viewModelScope.launch { imageIndexingCoordinator.cancel() }
    }

    fun retryFailedIndexing() {
        viewModelScope.launch { imageIndexingCoordinator.retryFailed() }
    }

    fun clearAllImageIndexes() {
        viewModelScope.launch { imageIndexingCoordinator.clearAll() }
    }

    // ===== 外观设置操作 =====

    /** 切换主题模式：立即持久化，全局即时生效，无需重启 App */
    fun onThemeModeSelect(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            themeRepository.saveThemeMode(mode)
        }
    }

    /** 更新全局文字颜色配置：立即持久化，首页/详情/输入框实时生效 */
    fun onTextColorConfigChange(config: TextColorConfig) {
        _textColorConfig.value = config
        viewModelScope.launch {
            themeRepository.saveTextColorConfig(config)
        }
    }

    // ===== AI 配置操作（只改工作副本）=====

    /**
     * 切换使用模式：免费 / 自定义（即时持久化，免费模式不写任何接口/模型/Key）。
     *
     * 从「自定义」切回「免费」视为明确的「放弃修改」动作：丢弃未保存的自定义配置，
     * 恢复到上次已保存的值，并把 hasChanges 归零，不再弹多余的「放弃修改」确认框。
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
                // 免费/自定义模式变化会换掉实际请求的 Provider 与模型：旧视觉能力缓存立即失效
                imageUnderstandingRepository.invalidateCapability()
            }
        } else {
            _mode.value = mode
            viewModelScope.launch {
                settingsRepository.saveMode(mode)
                imageUnderstandingRepository.invalidateCapability()
            }
        }
    }

    /** 选择预设：填入 Base URL / 模型列表，并恢复该预设已保存的模型版本与 API Key */
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

    /** 切换 API Key 可见性（仅 UI，不计入变更）*/
    fun toggleApiKeyVisible() {
        _editState.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }
    }

    /** 保存配置并持久化：仅此方法写入 Repository */
    fun saveAiConfig() {
        val state = _editState.value
        if (state.baseUrl.isBlank() || state.modelName.isBlank() || state.apiKey.isBlank()) {
            _saveMessage.value = UiMessage.Res(R.string.settings_ai_config_incomplete)
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
            // Base URL / 模型 / Key 变化后旧视觉能力缓存不再可信，需用户重新探测
            imageUnderstandingRepository.invalidateCapability()
            _saveMessage.value = UiMessage.Res(R.string.settings_ai_config_saved)
        }
    }

    // 一次性标志：从失败引导「去配置 API」进入时，自动切到自定义模式并定位到 AI 配置卡
    private val _autoOpenAiConfig = MutableStateFlow(false)
    val autoOpenAiConfig: StateFlow<Boolean> = _autoOpenAiConfig.asStateFlow()

    /** 「去配置 API」入口调用：切到自定义模式（持久化，便于继续编辑/保存），回到设置页自动定位。 */
    fun markAutoOpenAiConfig() {
        if (_mode.value != AiConfig.MODE_CUSTOM) {
            _mode.value = AiConfig.MODE_CUSTOM
            viewModelScope.launch { settingsRepository.saveMode(AiConfig.MODE_CUSTOM) }
        }
        _autoOpenAiConfig.value = true
    }

    /** 消费自动定位标志。 */
    fun consumeAutoOpenAiConfig() {
        _autoOpenAiConfig.value = false
    }

    /** 消费 Snackbar 消息 */
    fun consumeSaveMessage() {
        _saveMessage.value = null
    }

    // ===== 首页搜索小贴士 =====

    /** 首页「搜索小贴士」弹窗是否启用 */
    fun isSearchTipsEnabled(): Boolean = appPreferencesRepository.getSearchTipsEnabled()

    /** 设置首页「搜索小贴士」弹窗是否启用 */
    fun setSearchTipsEnabled(enabled: Boolean) {
        appPreferencesRepository.setSearchTipsEnabled(enabled)
    }

    // ===== 数据备份与恢复 =====

    // 导出 / 导入进行中（显示进度对话框并阻止用户操作）
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    // 导出文件名输入对话框
    private val _showExportNameDialog = MutableStateFlow(false)
    val showExportNameDialog: StateFlow<Boolean> = _showExportNameDialog.asStateFlow()

    // 只保存时间戳：默认文件名由设置页用字符串资源按当前语言拼接（避免缓存旧语言文案）
    private val _defaultExportTimestamp = MutableStateFlow("")
    val defaultExportTimestamp: StateFlow<String> = _defaultExportTimestamp.asStateFlow()

    // 待导入备份信息（非 null 时显示导入确认对话框）
    private val _pendingImportInfo = MutableStateFlow<BackupInfo?>(null)
    val pendingImportInfo: StateFlow<BackupInfo?> = _pendingImportInfo.asStateFlow()
    private var pendingImportUri: Uri? = null

    // 备份操作结果消息（Snackbar 展示）；只存资源 ID + 参数，由设置页按当前语言解析
    private val _backupMessage = MutableStateFlow<UiMessage?>(null)
    val backupMessage: StateFlow<UiMessage?> = _backupMessage.asStateFlow()

    /** 点击「导出数据」：弹出文件名输入对话框（默认带时间戳） */
    fun onExportClick() {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        _defaultExportTimestamp.value = time
        _showExportNameDialog.value = true
    }

    /** 关闭导出文件名对话框 */
    fun onExportNameDismiss() {
        _showExportNameDialog.value = false
    }

    /** 确认导出：写入用户选择的保存位置 */
    fun onExportNameConfirmed(saveUri: Uri?) {
        _showExportNameDialog.value = false
        val uri = saveUri ?: return
        _backupBusy.value = true
        viewModelScope.launch {
            backupRepository.exportData(uri)
                .onSuccess { result ->
                    _backupMessage.value = UiMessage.Res(
                        R.string.settings_backup_export_success,
                        listOf(result.itemCount),
                    )
                }
                .onFailure { e ->
                    _backupMessage.value = UiMessage.Res(
                        R.string.settings_backup_export_failed,
                        listOf(UiMessage.Res(R.string.settings_backup_unknown_error)),
                    )
                }
            _backupBusy.value = false
        }
    }

    /** 选择备份文件后：解析备份信息，准备导入确认 */
    fun onImportFileSelected(uri: Uri) {
        _backupBusy.value = true
        viewModelScope.launch {
            backupRepository.parseBackupInfo(uri)
                .onSuccess { info ->
                    pendingImportUri = uri
                    _pendingImportInfo.value = info
                }
                .onFailure { e ->
                    _backupMessage.value = UiMessage.Res(
                        R.string.settings_backup_import_failed,
                        listOf(UiMessage.Res(R.string.settings_backup_invalid)),
                    )
                }
            _backupBusy.value = false
        }
    }

    /** 关闭导入确认对话框 */
    fun onImportDismiss() {
        pendingImportUri = null
        _pendingImportInfo.value = null
    }

    /** 选择导入模式并执行导入（覆盖 / 合并） */
    fun onImportModeSelected(mode: ImportMode) {
        val uri = pendingImportUri ?: return
        pendingImportUri = null
        _pendingImportInfo.value = null
        _backupBusy.value = true
        viewModelScope.launch {
            backupRepository.importData(uri, mode)
                .onSuccess { result ->
                    val modeRes = if (mode == ImportMode.OVERWRITE) {
                        R.string.import_mode_overwrite
                    } else {
                        R.string.import_mode_merge
                    }
                    _backupMessage.value = UiMessage.Res(
                        R.string.settings_backup_import_success,
                        listOf(
                            UiMessage.Res(modeRes),
                            result.importedItems,
                            result.skippedItems,
                        ),
                    )
                }
                .onFailure { e ->
                    _backupMessage.value = UiMessage.Res(
                        R.string.settings_backup_import_failed,
                        listOf(UiMessage.Res(R.string.settings_backup_unknown_error)),
                    )
                }
            _backupBusy.value = false
        }
    }

    /** 消费备份结果消息 */
    fun consumeBackupMessage() {
        _backupMessage.value = null
    }
}

/** AI 智能解析配置编辑工作副本（UI 唯一绑定源）*/
data class SettingsEditState(
    /** 当前预设名称 */
    val presetType: String = LlmPreset.DEEPSEEK.label,

    /** 接口地址 */
    val baseUrl: String = LlmPreset.DEEPSEEK.baseUrl,

    /** 模型名称 */
    val modelName: String = LlmPreset.DEEPSEEK.model,

    /** API Key */
    val apiKey: String = "",

    /** API Key 是否明文显示（仅 UI，不计入内容变更）*/
    val apiKeyVisible: Boolean = false,
)

/** 设置页 AI 图片理解操作状态（失败携带显示层资源消息，由设置页按当前语言解析）。 */
sealed interface VisionActionState {
    /** 空闲。 */
    data object Idle : VisionActionState

    /** 正在用合成图探测视觉能力。 */
    data object Probing : VisionActionState

    /** 探测失败：展示可操作提示，用户关闭后回到 [Idle]。 */
    data class Failed(val message: UiMessage) : VisionActionState
}
