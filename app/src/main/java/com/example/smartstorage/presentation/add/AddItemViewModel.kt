package com.example.smartstorage.presentation.add

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.image.ImageStorage
import com.example.smartstorage.data.local.prefs.AppPreferencesRepository
import com.example.smartstorage.data.remote.llm.LlmClient
import com.example.smartstorage.data.remote.llm.LlmTimeoutException
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.usecase.AddItemUseCase
import com.example.smartstorage.domain.usecase.GetItemByNameUseCase
import com.example.smartstorage.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val updateItemUseCase: UpdateItemUseCase,
    private val getItemByNameUseCase: GetItemByNameUseCase,
    private val llmClient: LlmClient,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val imageStorage: ImageStorage,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

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

    // 免费模式解析超时提示（true 时 UI 显示“解析超时”对话框）
    private val _timeoutDialog = MutableStateFlow(false)
    val timeoutDialog: StateFlow<Boolean> = _timeoutDialog.asStateFlow()

    /** 关闭“解析超时”对话框。 */
    fun consumeTimeout() {
        _timeoutDialog.value = false
    }

    // 输入文字颜色（设置页可个性化）
    private val _inputTextColor = MutableStateFlow(AppPreferencesRepository.DEFAULT_INPUT_TEXT_COLOR)
    val inputTextColor: StateFlow<Int> = _inputTextColor.asStateFlow()

    init {
        _inputTextColor.value = appPreferencesRepository.getInputTextColor()
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
        _timeoutDialog.value = false
        _inputTextColor.value = appPreferencesRepository.getInputTextColor()
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
            Toast.makeText(context, "最多添加 $MAX_IMAGES 张照片", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "图片保存失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /** 拍照完成：同选图，压缩保存后追加到列表。 */
    fun onImageFileSelected(file: File) {
        viewModelScope.launch {
            runCatching { imageStorage.saveFromFile(file) }
                .onSuccess { appendImage(it) }
                .onFailure { e ->
                    Toast.makeText(context, "图片保存失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /** 移除指定位置的图片：仅标记（原始图计入 removedImagePaths），文件删除延迟到保存时执行。 */
    fun removeImageAt(index: Int) {
        updateState { state ->
            val removed = state.currentImagePaths.getOrNull(index) ?: return@updateState state
            val removedList = if (removed in state.originalImagePaths) {
                state.removedImagePaths + removed
            } else {
                state.removedImagePaths
            }
            state.copy(
                currentImagePaths = state.currentImagePaths.filterIndexed { i, _ -> i != index },
                removedImagePaths = removedList,
            )
        }
    }

    /**
     * 用已配置的大模型把口语描述解析为物品字段并填入表单。
     *
     * 注意：解析与“保存时的查重”（save → getItemByName）调用链相互独立；
     * 解析失败只置错误态，绝不影响用户手动填写与后续保存。
     */
    fun parseDescription() {
        val text = _editState.value.voiceDescription.trim()
        if (text.isEmpty()) {
            _parseState.value = LlmParseState.Error("请先输入或语音录入描述")
            return
        }
        _parseState.value = LlmParseState.Parsing
        viewModelScope.launch {
            runCatching { llmClient.parseItem(text) }
                .onSuccess { parsed ->
                    if (parsed == null) {
                        _parseState.value = LlmParseState.Error("AI 解析失败或未配置，请到设置页检查 API 配置")
                    } else {
                        updateState {
                            it.copy(name = parsed.name, location = parsed.location, desc = parsed.description)
                        }
                        _parseState.value = LlmParseState.Success
                    }
                }
                .onFailure { e ->
                    if (e is LlmTimeoutException) {
                        // 免费模式超时：弹“解析超时”引导
                        _timeoutDialog.value = true
                        _parseState.value = LlmParseState.Error("解析超时，可稍后重试或切换到自定义模式")
                    } else {
                        _parseState.value = LlmParseState.Error("解析失败：${e.message ?: "未知错误"}")
                    }
                }
        }
    }

    /** 保存：先按名称查重，无重名或编辑自身直接写入；有重名则弹窗让用户选择。 */
    fun save() {
        val state = _editState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _saveState.value = SaveState.Error("物品名不能为空")
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
                    _saveState.value = SaveState.Error("查重失败：${e.message ?: "未知错误"}")
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
            }.onFailure {
                _saveState.value = SaveState.Error("保存失败：${it.message ?: "未知错误"}")
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
sealed interface LlmParseState {
    /** 初始空闲 */
    data object Idle : LlmParseState

    /** 解析中 */
    data object Parsing : LlmParseState

    /** 解析成功 */
    data object Success : LlmParseState

    /** 解析失败 */
    data class Error(val message: String) : LlmParseState
}

/** 保存流程状态。 */
sealed interface SaveState {
    /** 初始空闲 */
    data object Idle : SaveState

    /** 保存中 */
    data object Saving : SaveState

    /** 保存成功 */
    data object Success : SaveState

    /** 保存失败 */
    data class Error(val message: String) : SaveState
}
