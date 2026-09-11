package com.example.smartstorage.presentation.add

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.image.ImageStorage
import com.example.smartstorage.data.local.prefs.STAR_REMIND_LATER_MILLIS
import com.example.smartstorage.data.local.prefs.StarMilestoneRepository
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.ThemeRepository
import com.example.smartstorage.data.remote.llm.LlmClient
import com.example.smartstorage.data.remote.llm.LlmTimeoutException
import com.example.smartstorage.data.remote.llm.ParseItemsResult
import com.example.smartstorage.domain.model.BatchDuplicateChoice
import com.example.smartstorage.domain.model.BatchDraftItem
import com.example.smartstorage.domain.model.BatchDraftOps
import com.example.smartstorage.domain.model.ParsedItem
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.usecase.AddItemUseCase
import com.example.smartstorage.domain.usecase.BatchAddItemsUseCase
import com.example.smartstorage.domain.usecase.BatchAddOutcome
import com.example.smartstorage.domain.usecase.GetItemByNameUseCase
import com.example.smartstorage.domain.usecase.UpdateItemUseCase
import com.example.smartstorage.R
import com.example.smartstorage.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

/**
 * 编辑状态工作副本：页面 UI 的唯一数据源。
 *
 * 进入页面时从数据库复制一份；未保存退出直接丢弃，不触碰数据库与文件；
 * 仅点击“保存”时才统一提交（删除被移除的图片文件 + 写数据库）。
 */
data class ItemEditState(
    /** 物品 ID（新增为 0） */
    val id: Long = 0L,

    /** 物品名 */
    val name: String = "",

    /** 存放地点 */
    val location: String = "",

    /** 备注 */
    val desc: String = "",

    /** 口语描述（语音/大模型解析用） */
    val voiceDescription: String = "",

    /** 当前界面展示的图片路径列表（可能尚未保存） */
    val currentImagePaths: List<String> = emptyList(),

    /** 进入页面时从数据库读到的原始图片路径列表 */
    val originalImagePaths: List<String> = emptyList(),

    /** 本次编辑中已移除的图片路径（保存时才真正删除文件） */
    val removedImagePaths: List<String> = emptyList(),
)

/**
 * 重复物品检测状态：保存时发现同名旧记录，弹窗让用户选择。
 *
 * @property existingItem 查到的重名旧记录
 * @property currentFormData 点击保存时的表单工作副本（用于展示/后续决策）
 */
data class DuplicateCheckState(
    val existingItem: Item,
    val currentFormData: ItemEditState,
)

/** 重复物品对话框的用户选择。 */
enum class DuplicateDecision {
    /** 更新旧记录（合并） */
    UPDATE_EXISTING,

    /** 新建记录（忽略重名直接插入） */
    INSERT_NEW,

    /** 取消：关闭对话框，停留在当前页面 */
    CANCEL,
}

/**
 * 添加/编辑物品 ViewModel：基于“工作副本”管理表单、多图与保存流程。
 */
@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val batchAddItemsUseCase: BatchAddItemsUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val getItemByNameUseCase: GetItemByNameUseCase,
    private val llmClient: LlmClient,
    private val themeRepository: ThemeRepository,
    private val starMilestoneRepository: StarMilestoneRepository,
    private val imageStorage: ImageStorage,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // 一次性 UI 消息（照片上限、图片保存失败等）：由 Compose 层用界面 Context 按当前语言解析
    private val _uiMessages = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages: Flow<UiMessage> = _uiMessages.receiveAsFlow()

    /** 最多可添加的照片数量。 */
    companion object {
        const val MAX_IMAGES = 9
    }

    // 进入编辑页时的原始物品（仅用于保存时 copy 保留 id/createdAt，不作为 UI 数据源）
    private var editingItem: Item? = null

    // 编辑状态工作副本（唯一 UI 数据源），构造时从 SavedStateHandle 恢复（旋转/进程重建）
    private val _editState = MutableStateFlow(
        ItemEditState(
            id = savedStateHandle.get<Long>("id") ?: 0L,
            name = savedStateHandle.get<String>("name") ?: "",
            location = savedStateHandle.get<String>("location") ?: "",
            desc = savedStateHandle.get<String>("desc") ?: "",
            voiceDescription = savedStateHandle.get<String>("voice_description") ?: "",
            currentImagePaths = decodePaths(savedStateHandle.get<String>("current_images")),
            originalImagePaths = decodePaths(savedStateHandle.get<String>("original_images")),
            removedImagePaths = decodePaths(savedStateHandle.get<String>("removed_images")),
        ),
    )
    val editState: StateFlow<ItemEditState> = _editState.asStateFlow()

    // 进入页面时的原始快照（用于判断是否有未保存修改）
    private val _original = MutableStateFlow<ItemEditState?>(null)

    // 是否有未保存的修改（同步计算，避免异步窗口导致退出确认弹窗重复出现）
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    /** 同步重算“是否有未保存修改”（data class 相等比较：名称/地点/备注/口语描述/图片）。 */
    private fun recomputeHasChanges() {
        val edit = _editState.value
        val original = _original.value
        _hasChanges.value = original != null && edit != original
    }

    // 保存流程状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 重复物品检测状态（非 null 时 UI 显示“物品已存在”对话框）
    private val _duplicateCheckState = MutableStateFlow<DuplicateCheckState?>(null)
    val duplicateCheckState: StateFlow<DuplicateCheckState?> = _duplicateCheckState.asStateFlow()

    // 大模型解析状态
    private val _parseState = MutableStateFlow<LlmParseState>(LlmParseState.Idle)
    val parseState: StateFlow<LlmParseState> = _parseState.asStateFlow()

    // AI 解析失败引导（true 时 UI 显示“AI 解析未成功”对话框）
    private val _parseFailedGuide = MutableStateFlow(false)
    val parseFailedGuide: StateFlow<Boolean> = _parseFailedGuide.asStateFlow()

    // 引导是否来自“超时且尚未本地降级”（点「稍后」时需先执行一次本地降级）
    private var guideNeedsLocalFallback = false

    // 本页面会话内是否已选择「本地识别继续」（避免同一会话内反复弹超时框）
    private var freeTimeoutFallbackChosen = false

    // ===== AI 批量解析 =====
    // 批量解析出的草稿条目（>1 条时进入批量模式；以 uid 为稳定标识，勾选/照片归属不依赖列表下标）
    private val _batchItems = MutableStateFlow<List<BatchDraftItem>>(emptyList())
    val batchItems: StateFlow<List<BatchDraftItem>> = _batchItems.asStateFlow()
    // 批量勾选状态（uid 集合，默认全选）
    private val _batchSelected = MutableStateFlow<Set<Long>>(emptySet())
    val batchSelected: StateFlow<Set<Long>> = _batchSelected.asStateFlow()
    // 是否显示批量确认对话框
    private val _showBatchDialog = MutableStateFlow(false)
    val showBatchDialog: StateFlow<Boolean> = _showBatchDialog.asStateFlow()
    // 与库中重名的物品名称集合（预查询，用于 UI 标记）
    private val _batchDuplicateNames = MutableStateFlow<Set<String>>(emptySet())
    val batchDuplicateNames: StateFlow<Set<String>> = _batchDuplicateNames.asStateFlow()
    // 批量处理中待用户决策的重名物品（非 null 时显示重复弹窗）
    private val _batchDuplicatePending = MutableStateFlow<BatchDuplicatePending?>(null)
    val batchDuplicatePending: StateFlow<BatchDuplicatePending?> = _batchDuplicatePending.asStateFlow()

    // 是否正在执行批量保存（防重复点击）
    private var _batchRunning = false

    // 批量弹窗内的提示数据（如部分保存失败时的计数与失败名单），文案由弹窗按当前语言拼接
    private val _batchNotice = MutableStateFlow<BatchNotice?>(null)
    val batchNotice: StateFlow<BatchNotice?> = _batchNotice.asStateFlow()

    // 本会话内批量保存已以“原路径”直存、被某条记录引用的草稿照片路径；
    // 部分失败重试时作为 alreadyClaimedPaths 传入，避免重试条目与已成功条目共享同一照片文件
    private val batchClaimedOriginals = mutableSetOf<String>()

    // 草稿条目 uid 分配器（进入批量模式时为每条识别结果分配新 uid，重新识别后不会串用旧归属）
    private var batchNextUid = 1L

    // 模型解析降级/失败警示（结构化：UI 按当前语言渲染；null 表示无）
    private val _parseWarning = MutableStateFlow<ParseWarningKind?>(null)
    val parseWarning: StateFlow<ParseWarningKind?> = _parseWarning.asStateFlow()

    /** 关闭失败引导（去配置 API 前调用）：停止任务、保留输入与本地结果，等待后续操作。 */
    fun dismissParseFailedGuide() {
        _parseFailedGuide.value = false
        if (_parseState.value == LlmParseState.Parsing) {
            _parseState.value = LlmParseState.Idle
        }
    }

    /**
     * 「稍后」：本次会话内不再反复弹引导；未降级（如超时）则补一次本地规则降级，
     * 已降级则只保留现有结果；不自动重试模型、不自动保存。
     */
    fun onParseFailedGuideLater() {
        freeTimeoutFallbackChosen = true
        _parseFailedGuide.value = false
        if (!guideNeedsLocalFallback) return
        guideNeedsLocalFallback = false
        val text = _editState.value.voiceDescription.trim()
        if (text.isEmpty()) {
            _parseState.value = LlmParseState.Error(ParseErrorKind.EMPTY_INPUT)
            return
        }
        if (_parseState.value == LlmParseState.Parsing) return
        _parseState.value = LlmParseState.Parsing
        viewModelScope.launch {
            applyParseResult(llmClient.parseItemsLocal(text), forcedLocal = true)
        }
    }

    // GitHub Star 里程碑提醒：待展示的里程碑（null 表示无）
    private val _starReminder = MutableStateFlow<Int?>(null)
    val starReminder: StateFlow<Int?> = _starReminder.asStateFlow()

    /**
     * 新增物品成功后：历史累计添加数 +1，并检查是否达到 GitHub Star 提醒里程碑。
     * 每次只弹一个未完成的里程碑（从小到大），处理完下次添加再检查下一个。
     */
    private fun checkStarMilestoneAfterAdd() {
        viewModelScope.launch {
            starMilestoneRepository.incrementTotalAdded()
            val state = starMilestoneRepository.readState()
            val milestone = state.pendingMilestone(System.currentTimeMillis())
            if (milestone != null) {
                _starReminder.value = milestone
            }
        }
    }

    /** 消费 Star 提醒状态（弹窗关闭后调用） */
    fun consumeStarReminder() {
        _starReminder.value = null
    }

    /** 点击「去 GitHub 点 Star」：标记该里程碑已永久提醒 */
    fun onStarGoToGithub(milestone: Int) {
        viewModelScope.launch {
            starMilestoneRepository.markReminded(milestone)
            _starReminder.value = null
        }
    }

    /** 点击「稍后提醒」或关闭弹窗：24 小时后再次提醒 */
    fun onStarRemindLater() {
        viewModelScope.launch {
            starMilestoneRepository.setRemindLater(System.currentTimeMillis() + STAR_REMIND_LATER_MILLIS)
            _starReminder.value = null
        }
    }

    // 全局文字颜色配置（设置页可个性化，实时生效）
    private val _textColorConfig = MutableStateFlow(TextColorConfig())
    val textColorConfig: StateFlow<TextColorConfig> = _textColorConfig.asStateFlow()

    init {
        // 订阅全局文字颜色：设置页修改后立即生效，无需重启 App
        viewModelScope.launch {
            themeRepository.textColorConfig.collect { _textColorConfig.value = it }
        }
    }

    /** 更新工作副本并同步到 SavedStateHandle。 */
    private fun updateState(transform: (ItemEditState) -> ItemEditState) {
        _editState.update { current ->
            val next = transform(current)
            savedStateHandle["id"] = next.id
            savedStateHandle["name"] = next.name
            savedStateHandle["location"] = next.location
            savedStateHandle["desc"] = next.desc
            savedStateHandle["voice_description"] = next.voiceDescription
            savedStateHandle["current_images"] = encodePaths(next.currentImagePaths)
            savedStateHandle["original_images"] = encodePaths(next.originalImagePaths)
            savedStateHandle["removed_images"] = encodePaths(next.removedImagePaths)
            next
        }
        recomputeHasChanges()
    }

    /** 丢弃所有未提交修改（未保存退出时调用）：不触碰数据库与文件。 */
    fun clearState() {
        editingItem = null
        _editState.value = ItemEditState()
        _original.value = null
        _saveState.value = SaveState.Idle
        _duplicateCheckState.value = null
        _parseState.value = LlmParseState.Idle
        _parseFailedGuide.value = false
        guideNeedsLocalFallback = false
        freeTimeoutFallbackChosen = false
        // 重置 AI 批量解析状态
        _batchItems.value = emptyList()
        _batchSelected.value = emptySet()
        _showBatchDialog.value = false
        _batchDuplicateNames.value = emptySet()
        _batchDuplicatePending.value = null
        _batchNotice.value = null
        batchClaimedOriginals.clear()
        _parseWarning.value = null
        recomputeHasChanges()
    }

    /** 进入编辑模式：把数据库数据复制到工作副本。 */
    fun loadItem(item: Item) {
        editingItem = item
        val snapshot = ItemEditState(
            id = item.id,
            name = item.name,
            location = item.location,
            desc = item.description,
            currentImagePaths = item.imagePaths,
            originalImagePaths = item.imagePaths,
        )
        _editState.value = snapshot
        _original.value = snapshot
        _saveState.value = SaveState.Idle
        recomputeHasChanges()
    }

    fun onNameChange(value: String) = updateState { it.copy(name = value) }

    fun onLocationChange(value: String) = updateState { it.copy(location = value) }

    fun onDescriptionChange(value: String) = updateState { it.copy(desc = value) }

    fun onVoiceDescriptionChange(value: String) = updateState { it.copy(voiceDescription = value) }

    /** 追加一张已保存好的图片到工作副本（最多 [MAX_IMAGES] 张）。 */
    private fun appendImage(newPath: String) {
        val current = _editState.value.currentImagePaths
        if (current.size >= MAX_IMAGES) {
            _uiMessages.trySend(UiMessage.Res(R.string.add_toast_max_photos, listOf(MAX_IMAGES)))
            return
        }
        updateState { it.copy(currentImagePaths = it.currentImagePaths + newPath) }
    }

    /** 从相册选图：压缩保存新文件并追加到列表；不删旧图、不写数据库。 */
    fun onImageUriSelected(uri: Uri) {
        viewModelScope.launch {
            runCatching { imageStorage.saveFromUri(uri) }
                .onSuccess { appendImage(it) }
                .onFailure { e ->
                    _uiMessages.trySend(UiMessage.Res(R.string.add_toast_img_save_failed))
                }
        }
    }

    /** 拍照完成：同选图，压缩保存后追加到列表。 */
    fun onImageFileSelected(file: File) {
        viewModelScope.launch {
            runCatching { imageStorage.saveFromFile(file) }
                .onSuccess { appendImage(it) }
                .onFailure { e ->
                    _uiMessages.trySend(UiMessage.Res(R.string.add_toast_img_save_failed))
                }
        }
    }

    /** 移除指定位置的图片：仅标记（原始图计入 removedImagePaths），文件删除延迟到保存时执行。 */
    fun removeImageAt(index: Int) {
        val removedPath = _editState.value.currentImagePaths.getOrNull(index) ?: return
        updateState { state ->
            val removedList = if (removedPath in state.originalImagePaths) {
                state.removedImagePaths + removedPath
            } else {
                state.removedImagePaths
            }
            state.copy(
                currentImagePaths = state.currentImagePaths.filterIndexed { i, _ -> i != index },
                removedImagePaths = removedList,
            )
        }
        // 照片池移除后，同步剥离所有批量草稿里对该路径的引用，避免保存悬空路径
        if (_batchItems.value.isNotEmpty()) {
            _batchItems.value = BatchDraftOps.detachPath(_batchItems.value, removedPath)
        }
    }

    /**
     * 用已配置的大模型把口语描述解析为物品字段并填入表单。
     *
     * 单条：自动填入表单（照片留在表单中，保存时自动关联）；多条：进入批量确认弹窗（可逐条编辑、逐张分配照片）；
     * 免费模型超时弹“解析超时”引导；模型不可用回退本地规则并给出明确警示（绝不显示“解析完成”冒充模型成功）；
     * 解析失败只置错误态，绝不影响用户手动填写、照片与后续保存。
     */
    fun parseDescription() {
        val text = _editState.value.voiceDescription.trim()
        if (text.isEmpty()) {
            _parseState.value = LlmParseState.Error(ParseErrorKind.EMPTY_INPUT)
            return
        }
        // 单飞：解析中忽略重复触发
        if (_parseState.value == LlmParseState.Parsing) return
        _parseState.value = LlmParseState.Parsing
        _parseWarning.value = null
        viewModelScope.launch {
            try {
                applyParseResult(llmClient.parseItems(text))
            } catch (e: LlmTimeoutException) {
                if (freeTimeoutFallbackChosen) {
                    // 本次会话已选择「稍后」：不再弹框，直接本地降级继续
                    applyParseResult(llmClient.parseItemsLocal(text), forcedLocal = true)
                } else {
                    // 免费模型超时且尚未本地降级：弹统一失败引导，点「稍后」时再降级
                    guideNeedsLocalFallback = true
                    _parseFailedGuide.value = true
                    _parseState.value = LlmParseState.Idle
                }
            } catch (e: Exception) {
                _parseState.value = LlmParseState.Error(ParseErrorKind.UNKNOWN)
            }
        }
    }

    /**
     * 把解析结果应用到表单/批量弹窗（模型结果与本地降级共用同一套处理，行为一致）。
     *
     * @param forcedLocal 免费模式超时后主动选择本地降级时传 true，用于展示对应的降级提示
     */
    private suspend fun applyParseResult(result: ParseItemsResult, forcedLocal: Boolean = false) {
        // 模型路径是否失败（非超时，已自动本地降级）：用于决定是否弹“AI 解析未成功”引导
        val modelFailed = !forcedLocal && result.warning != null
        when {
            result.items.isEmpty() -> {
                // 未识别出有效物品：保留原文并明确提示，不显示“解析完成”
                _parseState.value = LlmParseState.Error(ParseErrorKind.EMPTY_RESULT)
            }
            result.items.size == 1 -> {
                val item = result.items.first()
                if (item.name.isBlank()) {
                    _parseState.value = LlmParseState.Error(ParseErrorKind.RESULT_NO_NAME)
                } else {
                    // 单条：自动填入表单（维持原有行为）
                    updateState {
                        it.copy(name = item.name, location = item.location, desc = item.description)
                    }
                    if (forcedLocal) {
                        _parseWarning.value = ParseWarningKind.FREE_LOCAL_SINGLE
                    } else if (result.warning != null) {
                        _parseWarning.value = ParseWarningKind.MODEL_FAIL_LOCAL_SINGLE
                    }
                    _parseState.value = LlmParseState.Success
                }
            }
            else -> {
                // 多条：进入批量确认弹窗；表单区不再显示“已自动填入表单”的误导提示
                enterBatchMode(result.items)
                if (forcedLocal) {
                    _parseWarning.value = ParseWarningKind.FREE_LOCAL_MULTI
                } else if (result.warning != null) {
                    _parseWarning.value = ParseWarningKind.MODEL_FAIL_LOCAL_MULTI
                }
                _parseState.value = LlmParseState.Idle
            }
        }
        // 非超时模型最终失败（已自动本地降级并展示）→ 首次弹统一失败引导
        if (modelFailed && !freeTimeoutFallbackChosen) {
            _parseFailedGuide.value = true
            guideNeedsLocalFallback = false
        }
    }

    /** 消费“解析降级警示”（用户关闭提示时调用）。 */    /** 消费“解析降级警示”（用户关闭提示时调用）。 */
    fun consumeParseWarning() {
        _parseWarning.value = null
    }

    /** 进入批量模式：为每条识别结果分配稳定 uid、默认全选、照片初始未分配，并预查重名物品。 */
    private fun enterBatchMode(items: List<ParsedItem>) {
        val drafts = items.map {
            BatchDraftItem(
                uid = batchNextUid++,
                name = it.name,
                location = it.location,
                description = it.description,
            )
        }
        _batchItems.value = drafts
        _batchSelected.value = drafts.map { it.uid }.toSet()
        _batchNotice.value = null
        _showBatchDialog.value = true
        refreshBatchDuplicateNames()
    }

    /** 切换某条草稿的勾选状态（按 uid）。 */
    fun onBatchSelectionChange(uid: Long, checked: Boolean) {
        val current = _batchSelected.value.toMutableSet()
        if (checked) current.add(uid) else current.remove(uid)
        _batchSelected.value = current
    }

    /** 修改某条草稿的字段（随输入实时更新，供弹窗内编辑）。 */
    fun updateBatchItem(uid: Long, name: String, location: String, desc: String) {
        if (_batchItems.value.none { it.uid == uid }) return
        _batchItems.value = BatchDraftOps.update(_batchItems.value, uid, name, location, desc)
        // 名称可能变化，重查“⚠ 已存在”标记
        refreshBatchDuplicateNames()
    }

    /** 删除某条草稿：仅移除条目本身，不删除照片文件；照片仍留在池中可重新分配给其它物品。 */
    fun removeBatchItem(uid: Long) {
        if (_batchItems.value.none { it.uid == uid }) return
        _batchItems.value = BatchDraftOps.remove(_batchItems.value, uid)
        _batchSelected.value = _batchSelected.value - uid
        refreshBatchDuplicateNames()
    }

    /**
     * 切换某条草稿对某张照片的归属（同一张照片可分给多个草稿；单件最多 [MAX_IMAGES] 张）。
     */
    fun toggleBatchItemPhoto(uid: Long, photoPath: String, assign: Boolean) {
        val draft = _batchItems.value.firstOrNull { it.uid == uid } ?: return
        if (assign && photoPath !in draft.photoPaths && draft.photoPaths.size >= MAX_IMAGES) {
            _uiMessages.trySend(UiMessage.Res(R.string.add_toast_batch_max_photos, listOf(MAX_IMAGES)))
            return
        }
        _batchItems.value = BatchDraftOps.setPhoto(_batchItems.value, uid, photoPath, assign)
    }

    /** 重查批量列表中与库中重名的名称（用于弹窗内“⚠ 已存在”标记）。 */
    private fun refreshBatchDuplicateNames() {
        viewModelScope.launch {
            val names = _batchItems.value.map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val dups = names.filter { name ->
                runCatching { getItemByNameUseCase(name) != null }.getOrDefault(false)
            }.toSet()
            _batchDuplicateNames.value = dups
        }
    }

    /** 关闭批量确认对话框（表单内容与草稿保留，可继续手动编辑或重新解析）。 */
    fun dismissBatchDialog() {
        _showBatchDialog.value = false
        _batchNotice.value = null
    }

    /** 批量添加：对勾选草稿逐条保存；重名逐个弹窗决策；部分失败保留失败与未勾选草稿（含照片归属）供修改重试。 */
    fun confirmBatchAdd() {
        val drafts = _batchItems.value
        val selected = _batchSelected.value
        if (selected.isEmpty() || _batchRunning) return
        // 防重复点击：保存期间忽略再次触发
        _batchRunning = true
        _showBatchDialog.value = false
        _saveState.value = SaveState.Saving
        _batchNotice.value = null
        viewModelScope.launch {
            // 已被既有记录占用的路径：编辑模式下原记录照片 + 此前成功批次已直存的路径
            val claimedSeed = batchClaimedOriginals + _editState.value.originalImagePaths
            val result = runCatching {
                batchAddItemsUseCase.execute(
                    drafts = drafts,
                    selected = selected,
                    alreadyClaimedPaths = claimedSeed,
                    resolveDuplicate = { existing, draft ->
                        // 重名：阻塞等待用户在重复弹窗中做出选择
                        awaitBatchDuplicateChoice(existing, draft)
                    },
                    copyFile = { path -> imageStorage.saveFromFile(File(path)) },
                )
            }
            result.onSuccess { outcome ->
                // 记录本次以原路径直存、已被记录引用的照片，供部分失败重试时避免重复占用
                batchClaimedOriginals += outcome.consumedOriginals
                // 每成功新增一件都累计历史添加数并检查 Star 里程碑（含部分成功场景）
                if (outcome.added > 0) bumpStarMilestone(outcome.added)
                if (outcome.failedUids.isEmpty()) {
                    // 全部成功：清空批量状态并驱动返回首页
                    _batchItems.value = emptyList()
                    _batchSelected.value = emptySet()
                    batchClaimedOriginals.clear()
                    _batchNotice.value = null
                    _parseWarning.value = null
                    _saveState.value = SaveState.Success
                } else {
                    // 部分失败：保留失败条目与未勾选条目（含各自照片归属），重开弹窗供修改后重试
                    val failedSet = outcome.failedUids.toSet()
                    val remaining = drafts.filter { it.uid in failedSet || it.uid !in selected }
                    _batchItems.value = remaining
                    _batchSelected.value = remaining.map { it.uid }.filter { it in selected }.toSet()
                    _batchNotice.value = buildBatchFailureNotice(outcome, drafts)
                    _showBatchDialog.value = true
                    _saveState.value = SaveState.Idle
                }
            }.onFailure { e ->
                // 整体异常（极少见）：保留原列表，重开弹窗并提示
                _batchNotice.value = BatchNotice(unknownFailure = true)
                _showBatchDialog.value = true
                _saveState.value = SaveState.Idle
            }
            _batchRunning = false
        }
    }

    /** 汇总批量保存失败结果（计数 + 失败名单；名单为用户输入的物品名，不翻译）。 */
    private fun buildBatchFailureNotice(outcome: BatchAddOutcome, drafts: List<BatchDraftItem>): BatchNotice {
        val failedNames = outcome.failedUids
            .mapNotNull { uid -> drafts.firstOrNull { it.uid == uid }?.name?.trim() }
            .filter { it.isNotEmpty() }
        return BatchNotice(
            added = outcome.added,
            updated = outcome.updated,
            skipped = outcome.skipped,
            failedNames = failedNames,
        )
    }

    /** 新增成功后累计历史添加数并检查 Star 里程碑（added 为本次成功新增件数）。 */
    private suspend fun bumpStarMilestone(added: Int) {
        if (added <= 0) return
        repeat(added) { starMilestoneRepository.incrementTotalAdded() }
        val state = starMilestoneRepository.readState()
        val milestone = state.pendingMilestone(System.currentTimeMillis())
        if (milestone != null) {
            _starReminder.value = milestone
        }
    }

    /** 批量重名：挂起等待用户在重复弹窗中做出选择（阻塞式，选择后才继续下一条）。 */
    private suspend fun awaitBatchDuplicateChoice(
        existing: Item,
        draft: BatchDraftItem,
    ): BatchDuplicateChoice {
        val deferred = CompletableDeferred<BatchDuplicateChoice>()
        _batchDuplicatePending.value = BatchDuplicatePending(
            existingItem = existing,
            newItem = draft,
            deferred = deferred,
        )
        return deferred.await()
    }

    /** 用户在批量重复弹窗中做出选择（点外部 / 返回键关闭视同跳过并继续）。 */
    fun onBatchDuplicateChoice(choice: BatchDuplicateChoice) {
        _batchDuplicatePending.value?.deferred?.complete(choice)
        _batchDuplicatePending.value = null
    }
    /** 保存：先按名称查重，无重名或编辑自身直接写入；有重名则弹窗让用户选择。 */
    fun save() {
        val state = _editState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _saveState.value = SaveState.Error(SaveErrorKind.NAME_EMPTY)
            return
        }
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            runCatching { getItemByNameUseCase(name) }
                .onSuccess { existing ->
                    // 编辑自身且名称未变：直接更新，不弹窗
                    if (existing == null || editingItem?.id == existing.id) {
                        performSave()
                    } else {
                        // 检测到重名：弹窗等待用户选择（更新旧记录 / 新建记录 / 取消）
                        _duplicateCheckState.value = DuplicateCheckState(existing, _editState.value)
                        _saveState.value = SaveState.Idle
                    }
                }
                .onFailure { e ->
                    _saveState.value = SaveState.Error(SaveErrorKind.DUPLICATE_CHECK_FAILED)
                }
        }
    }

    /** 重复物品对话框按钮回调。 */
    fun onDuplicateDecision(decision: DuplicateDecision) {
        val dup = _duplicateCheckState.value ?: return
        _duplicateCheckState.value = null
        when (decision) {
            DuplicateDecision.UPDATE_EXISTING -> performSave(mergeInto = dup.existingItem)
            DuplicateDecision.INSERT_NEW -> performSave(forceInsert = true)
            DuplicateDecision.CANCEL -> Unit // 停留在当前页面，不保存
        }
    }

    /**
     * 统一执行持久化（只在“保存”或用户确认合并/新建时调用）。
     *
     * @param mergeInto 非空表示把当前表单合并进重名旧记录（保留其 id/createdAt）
     * @param forceInsert true 表示用户选择“新建记录”（忽略重名直接插入）
     */
    private fun performSave(mergeInto: Item? = null, forceInsert: Boolean = false) {
        val state = _editState.value
        val name = state.name.trim()
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            val result = runCatching {
                when {
                    // 合并：更新重名旧记录，保留其 id/createdAt；原记录保留、不删其图片
                    mergeInto != null -> {
                        val finalImages = state.currentImagePaths
                        // 旧记录里不在最终列表中的图片清理（被替换掉的）
                        mergeInto.imagePaths.filter { it !in finalImages }
                            .forEach { imageStorage.deleteImage(it) }
                        updateItemUseCase(
                            mergeInto.copy(
                                location = state.location.trim(),
                                description = state.desc.trim(),
                                imagePaths = finalImages,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }

                    // 用户选择“新建记录”：忽略重名直接插入
                    forceInsert -> {
                        // 若列表里仍有编辑中原记录的图片，先复制独立文件，避免两条记录共享图片文件
                        val finalImages = state.currentImagePaths.map { path ->
                            if (path in state.originalImagePaths) {
                                imageStorage.saveFromFile(File(path))
                            } else {
                                path
                            }
                        }
                        addItemUseCase(
                            Item(
                                name = name,
                                location = state.location.trim(),
                                description = state.desc.trim(),
                                imagePaths = finalImages,
                            ),
                        )
                    }

                    // 编辑自身：保留 ID 与创建时间
                    editingItem != null -> {
                        val current = editingItem!!
                        // 仅在保存时删除本次编辑中移除的图片文件
                        state.removedImagePaths.forEach { imageStorage.deleteImage(it) }
                        updateItemUseCase(
                            current.copy(
                                name = name,
                                location = state.location.trim(),
                                description = state.desc.trim(),
                                imagePaths = state.currentImagePaths,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }

                    // 新增（无重名）
                    else -> {
                        addItemUseCase(
                            Item(
                                name = name,
                                location = state.location.trim(),
                                description = state.desc.trim(),
                                imagePaths = state.currentImagePaths,
                            ),
                        )
                    }
                }
            }
            result.onSuccess {
                _saveState.value = SaveState.Success
                // 仅新增成功（新建 / 强制新建）时累计历史添加数并检查 Star 里程碑；合并 / 编辑自身不计数
                val isNewAdd = mergeInto == null && (forceInsert || editingItem == null)
                if (isNewAdd) {
                    checkStarMilestoneAfterAdd()
                }
            }.onFailure {
                _saveState.value = SaveState.Error(SaveErrorKind.SAVE_FAILED)
            }
        }
    }

    private fun encodePaths(paths: List<String>): String = JSONArray(paths).toString()

    private fun decodePaths(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}

/** 大模型解析状态。 */

/** 批量处理中待用户决策的重名物品（existingItem 旧记录、newItem 新解析、deferred 等待用户选择）。 */
data class BatchDuplicatePending(
    val existingItem: Item,
    val newItem: BatchDraftItem,
    val deferred: CompletableDeferred<BatchDuplicateChoice>,
)
sealed interface LlmParseState {
    /** 初始空闲 */
    data object Idle : LlmParseState

    /** 解析中 */
    data object Parsing : LlmParseState

    /** 解析成功 */
    data object Success : LlmParseState

    /** 解析失败（kind 由 UI 按当前语言渲染，避免语言切换后残留旧文本） */
    data class Error(val kind: ParseErrorKind) : LlmParseState
}

/** 解析失败类型。 */
enum class ParseErrorKind {
    /** 输入为空（前置校验） */
    EMPTY_INPUT,
    /** 未能识别出有效物品（含模型失败后本地降级仍为空） */
    EMPTY_RESULT,
    /** 识别结果缺少物品名 */
    RESULT_NO_NAME,
    /** 其它解析异常 */
    UNKNOWN,
}

/** 解析降级/失败橙色警示类型（本地规则结果提示，UI 按语言渲染）。 */
enum class ParseWarningKind {
    FREE_LOCAL_SINGLE,
    FREE_LOCAL_MULTI,
    MODEL_FAIL_LOCAL_SINGLE,
    MODEL_FAIL_LOCAL_MULTI,
}

/**
 * 批量保存结果提示数据：只携带计数与失败名单，文案由批量弹窗按当前语言拼接。
 *
 * @param unknownFailure 整体异常（未拿到逐条结果）时置 true，显示通用失败提示。
 */
data class BatchNotice(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failedNames: List<String> = emptyList(),
    val unknownFailure: Boolean = false,
)

/** 保存流程状态。 */
sealed interface SaveState {
    /** 初始空闲 */
    data object Idle : SaveState

    /** 保存中 */
    data object Saving : SaveState

    /** 保存成功 */
    data object Success : SaveState

    /** 保存失败（kind 由 UI 按当前语言渲染） */
    data class Error(val kind: SaveErrorKind) : SaveState
}

/** 保存失败类型。 */
enum class SaveErrorKind {
    /** 物品名不能为空 */
    NAME_EMPTY,
    /** 查重失败 */
    DUPLICATE_CHECK_FAILED,
    /** 其它保存失败 */
    SAVE_FAILED,
}
