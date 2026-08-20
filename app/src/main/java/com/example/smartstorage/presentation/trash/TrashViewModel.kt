package com.example.smartstorage.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.domain.usecase.EmptyTrashUseCase
import com.example.smartstorage.domain.usecase.ObserveTrashItemsUseCase
import com.example.smartstorage.domain.usecase.PermanentDeleteItemUseCase
import com.example.smartstorage.domain.usecase.RestoreItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 回收站 ViewModel：观察回收站物品，支持恢复、永久删除与清空。
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    observeTrashItemsUseCase: ObserveTrashItemsUseCase,
    private val restoreItemUseCase: RestoreItemUseCase,
    private val permanentDeleteItemUseCase: PermanentDeleteItemUseCase,
    private val emptyTrashUseCase: EmptyTrashUseCase,
) : ViewModel() {

    /** 回收站物品列表：订阅期间自动加载，操作后 Room Flow 自动刷新。 */
    val trashItems: StateFlow<List<Item>> = observeTrashItemsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 恢复：将物品移回主列表。 */
    fun restore(item: Item) {
        viewModelScope.launch {
            runCatching { restoreItemUseCase(item) }
        }
    }

    /** 永久删除：物理删除记录与关联照片文件。 */
    fun permanentDelete(item: Item) {
        viewModelScope.launch {
            runCatching { permanentDeleteItemUseCase(item) }
        }
    }

    /** 清空回收站：批量物理删除全部已删除记录与照片文件。 */
    fun emptyTrash() {
        viewModelScope.launch {
            runCatching { emptyTrashUseCase() }
        }
    }
}
