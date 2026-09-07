package com.example.smartstorage.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.ThemeRepository
import com.example.smartstorage.data.local.prefs.AppPreferencesRepository
import com.example.smartstorage.data.remote.llm.LlmClient
import com.example.smartstorage.data.remote.llm.LlmTimeoutException
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.usecase.DeleteItemUseCase
import com.example.smartstorage.domain.usecase.ObserveActiveCountUseCase
import com.example.smartstorage.domain.usecase.ObserveItemsUseCase
import com.example.smartstorage.domain.usecase.RestoreItemUseCase
import com.example.smartstorage.domain.usecase.SearchItemsByFieldsUseCase
import com.example.smartstorage.domain.usecase.SearchItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页（清单）ViewModel：
 * - 普通搜索：按关键词实时过滤；
 * - AI 语义搜索：把语音/输入的整句话解析为“名称/地点/描述”三字段，按三字段精确筛选；
 * - 删除（移入回收站）与撤销。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    observeActiveCountUseCase: ObserveActiveCountUseCase,
    private val searchItemsUseCase: SearchItemsUseCase,
    private val searchItemsByFieldsUseCase: SearchItemsByFieldsUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val restoreItemUseCase: RestoreItemUseCase,
    private val llmClient: LlmClient,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {


    // 全局文字颜色配置（订阅 ThemeRepository 实时流：设置页改色后首页立即生效）
    val textColorConfig: StateFlow<TextColorConfig> = themeRepository.textColorConfig
    // 搜索框文本（可手动输入，也可用键盘语音说出整句话）
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // AI 三字段精确筛选条件（null=未启用，走普通关键词搜索）
    private val _aiFilter = MutableStateFlow<AiFilter?>(null)
    val aiFilter: StateFlow<AiFilter?> = _aiFilter.asStateFlow()

    // 物品清单：AI 筛选优先；否则按关键词实时过滤（空白=全部）
    val items: StateFlow<List<Item>> = combine(_searchQuery, _aiFilter) { query, filter ->
        when {
            filter != null -> searchItemsByFieldsUseCase(filter.name, filter.location, filter.description)
            query.isBlank() -> observeItemsUseCase()
            else -> searchItemsUseCase(query)
        }
    }.flatMapLatest { it }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    // 是否从未添加过任何物品（用于区分“初始空状态”与“搜索无结果”）
    val isInitialEmpty: StateFlow<Boolean> = observeActiveCountUseCase()
        .map { it == 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    // AI 解析状态（用于按钮加载圈/失败提示）
    private val _parseState = MutableStateFlow<SearchParseState>(SearchParseState.Idle)
    val parseState: StateFlow<SearchParseState> = _parseState.asStateFlow()

    // AI 解析失败引导（true 时 UI 显示“AI 解析未成功”对话框）
    private val _parseFailedGuide = MutableStateFlow(false)
    val parseFailedGuide: StateFlow<Boolean> = _parseFailedGuide.asStateFlow()

    // 本页面会话内是否已选择本地降级（首页=直接用原文关键词搜索），避免同一会话反复弹超时框
    private var freeSearchTimeoutFallback = false

    /** 关闭失败引导（去配置 API 前调用）：仅关闭，不改输入与状态。 */
    fun dismissParseFailedGuide() {
        _parseFailedGuide.value = false
    }

    /** 「稍后」：本次会话内不再重复弹框，直接保留原文关键词搜索。 */
    fun onParseFailedGuideLater() {
        freeSearchTimeoutFallback = true
        _parseFailedGuide.value = false
        // 原文保留在搜索框，自动按关键词搜索；错误提示告知用户当前为原文搜索结果
        _parseState.value = SearchParseState.Error
    }

    // 最近一次软删除的物品（用于 Snackbar 撤销），null 表示无
    // 搜索框输入代数：解析结果返回时若代数已变化则丢弃（旧请求迟到不得覆盖新输入）
    private var queryGeneration = 0L

    private val _undoEvent = MutableStateFlow<Item?>(null)
    val undoEvent: StateFlow<Item?> = _undoEvent.asStateFlow()

    /** 更新搜索框文本（手动输入时退出 AI 筛选，回到普通关键词搜索）。 */
    fun onSearchQueryChange(query: String) {
        // 输入变化代数：用于丢弃迟到的旧解析结果
        queryGeneration++
        _searchQuery.value = query
        _aiFilter.value = null
        if (_parseState.value == SearchParseState.Error) {
            _parseState.value = SearchParseState.Idle
        }
    }

    /** “智能解析”按钮：把整句话解析为名称/地点/描述，并按三字段精确筛选。 */
    fun onAiParse() {
        val raw = _searchQuery.value.trim()
        if (raw.isEmpty() || _parseState.value == SearchParseState.Parsing) return
        // 记录本次解析对应的输入代数：若解析期间用户修改了输入则丢弃结果
        val generation = queryGeneration
        _parseState.value = SearchParseState.Parsing
        viewModelScope.launch {
            when (val outcome = llmClient.parseSearchKeyword(raw)) {
                is com.example.smartstorage.data.remote.llm.SearchParseOutcome.Keyword -> {
                    if (queryGeneration != generation) {
                        // 用户已修改输入：丢弃迟到的旧结果，不覆盖新输入
                        _parseState.value = SearchParseState.Idle
                        return@launch
                    }
                    if (outcome.text.isNotBlank()) {
                        // 提取到关键词：填入搜索框并触发实时搜索
                        _aiFilter.value = null
                        _searchQuery.value = outcome.text
                        _parseState.value = SearchParseState.Idle
                    } else {
                        // 未提取到关键词：保持原文搜索
                        _searchQuery.value = raw
                        _parseState.value = SearchParseState.Error
                    }
                }
                is com.example.smartstorage.data.remote.llm.SearchParseOutcome.Failed -> {
                    if (queryGeneration != generation) {
                        // 用户已修改输入：丢弃迟到的旧错误，不弹引导框
                        _parseState.value = SearchParseState.Idle
                        return@launch
                    }
                    // 模型/请求最终失败：保持原文搜索
                    _searchQuery.value = raw
                    _parseState.value = SearchParseState.Error
                    if (!freeSearchTimeoutFallback) {
                        // 本次会话未选过「稍后」：弹“AI 解析未成功”引导
                        _parseFailedGuide.value = true
                    }
                }
            }
        }
    }

    /** 清除 AI 三字段筛选，回到关键词搜索。 */    /** 清除 AI 三字段筛选，回到关键词搜索。 */    /** 清除 AI 三字段筛选，回到关键词搜索。 */
    fun clearAiFilter() {
        _aiFilter.value = null
    }

    /** 首页“搜索小贴士”弹窗是否启用（每次实时读，避免与设置页状态不同步）。 */
    fun isSearchTipsEnabled(): Boolean = appPreferencesRepository.getSearchTipsEnabled()

    /** 设置首页“搜索小贴士”弹窗是否启用。 */
    fun setSearchTipsEnabled(enabled: Boolean) {
        appPreferencesRepository.setSearchTipsEnabled(enabled)
    }

    /** 删除物品：移入回收站（Room Flow 自动刷新列表，不删照片）。 */
    fun delete(item: Item) {
        viewModelScope.launch {
            runCatching { deleteItemUseCase(item) }
                .onSuccess { _undoEvent.value = item }
        }
    }

    /** 撤销删除：把刚移入回收站的物品恢复回主列表。 */
    fun undoDelete() {
        val item = _undoEvent.value ?: return
        viewModelScope.launch {
            runCatching { restoreItemUseCase(item) }
            _undoEvent.value = null
        }
    }

    /** 消费撤销事件（Snackbar 消失或已处理时调用）。 */
    fun consumeUndo() {
        _undoEvent.value = null
    }
}

/** AI 三字段精确筛选条件（空字段表示不限制）。 */
data class AiFilter(
    val name: String,
    val location: String,
    val description: String,
)

/** 首页 AI 解析状态。 */
sealed interface SearchParseState {
    /** 空闲 */
    data object Idle : SearchParseState

    /** 解析中（按钮显示加载圈） */
    data object Parsing : SearchParseState

    /** 解析失败（已按原文搜索或提示换说法） */
    data object Error : SearchParseState
}
