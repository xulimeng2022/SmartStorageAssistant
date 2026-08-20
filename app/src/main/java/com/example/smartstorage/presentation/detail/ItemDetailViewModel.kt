package com.example.smartstorage.presentation.detail

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.data.local.image.ImageStorage
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.usecase.DeleteItemUseCase
import com.example.smartstorage.domain.usecase.ObserveItemByIdUseCase
import com.example.smartstorage.domain.usecase.UpdateItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 物品详情页 ViewModel：实时观察单个物品（编辑/照片增删后自动刷新）+ 删除 + 照片管理。
 */
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val observeItemByIdUseCase: ObserveItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val imageStorage: ImageStorage,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _item = MutableStateFlow<Item?>(null)
    val item: StateFlow<Item?> = _item.asStateFlow()

    private var currentId: Long = 0L
    private var observeJob: Job? = null

    /** 最多可添加的照片数量。 */
    private companion object {
        const val MAX_IMAGES = 9
    }

    /** 加载并订阅指定物品；重复进入同一物品时复用。 */
    fun load(id: Long) {
        if (currentId == id && observeJob?.isActive == true) return
        currentId = id
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeItemByIdUseCase(id).collect { _item.value = it }
        }
    }

    /** 删除物品，成功后回调。 */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val current = _item.value ?: return@launch
            runCatching { deleteItemUseCase(current) }
                .onSuccess { onDeleted() }
        }
    }

    /** 删除某张照片：从 imagePaths 移除 + 同步删除本地文件 + 更新数据库（Room Flow 自动刷新）。 */
    fun removePhoto(path: String) {
        viewModelScope.launch {
            val current = _item.value ?: return@launch
            if (path !in current.imagePaths) return@launch
            runCatching {
                imageStorage.deleteImage(path)
                updateItemUseCase(
                    current.copy(imagePaths = current.imagePaths - path),
                )
            }
        }
    }

    /** 从相册选图：压缩保存后追加到照片列表并更新数据库。 */
    fun addPhoto(uri: Uri) {
        saveAndAddPhoto { imageStorage.saveFromUri(uri) }
    }

    /** 拍照完成：压缩保存后追加到照片列表并更新数据库。 */
    fun addPhoto(file: File) {
        saveAndAddPhoto { imageStorage.saveFromFile(file) }
    }

    private fun saveAndAddPhoto(save: () -> String) {
        viewModelScope.launch {
            val current = _item.value ?: return@launch
            if (current.imagePaths.size >= MAX_IMAGES) {
                Toast.makeText(context, "最多添加 $MAX_IMAGES 张照片", Toast.LENGTH_SHORT).show()
                return@launch
            }
            runCatching { save() }
                .onSuccess { newPath ->
                    runCatching {
                        updateItemUseCase(
                            current.copy(imagePaths = current.imagePaths + newPath),
                        )
                    }
                }
                .onFailure { e ->
                    Toast.makeText(context, "图片保存失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
