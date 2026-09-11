package com.example.smartstorage.presentation.trash
import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.presentation.common.EmojiIconButton
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 回收站页面：展示已软删除物品，支持恢复、永久删除与清空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()

    // 清空回收站确认框
    var showEmptyConfirm by remember { mutableStateOf(false) }
    // 单个永久删除确认框
    var pendingPermanentDelete by remember { mutableStateOf<Item?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏：返回 + 清空回收站
        TopAppBar(
            title = { Text(stringResource(R.string.trash)) },
            navigationIcon = {
                EmojiIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { showEmptyConfirm = true },
                    enabled = trashItems.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.trash_empty_action), color = MaterialTheme.colorScheme.error)
                }
            },
        )

        if (trashItems.isEmpty()) {
            // 空状态占位
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.trash_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.trash_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // 回收站物品列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(trashItems, key = { it.id }) { item ->
                    TrashCard(
                        item = item,
                        onRestore = { viewModel.restore(item) },
                        onPermanentDelete = { pendingPermanentDelete = item },
                    )
                }
            }
        }
    }

    // 单个永久删除确认
    pendingPermanentDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text(stringResource(R.string.common_permanent)) },
            text = { Text(stringResource(R.string.trash_permanent_message, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentDelete(item)
                        pendingPermanentDelete = null
                    },
                ) {
                    Text(stringResource(R.string.common_permanent), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPermanentDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // 清空回收站确认
    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.trash_empty_action)) },
            text = { Text(stringResource(R.string.trash_clear_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyConfirm = false
                    },
                ) {
                    Text(stringResource(R.string.trash_clear_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/**
 * 回收站物品卡片：缩略图 + 名称/地点/删除时间 + 恢复/永久删除按钮。
 */
@Composable
private fun TrashCard(
    item: Item,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 缩略图（有图才显示）
            val thumbPath = item.imagePath
            if (!thumbPath.isNullOrBlank()) {
                AsyncImage(
                    model = File(thumbPath),
                    contentDescription = stringResource(R.string.home_cd_item_photo),
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 文本信息区
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.location.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.trash_location_format, item.location),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.trash_deleted_at_format, formatTime(item.deletedAt ?: 0L)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 恢复
            EmojiIconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = stringResource(R.string.trash_restore),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // 永久删除
            EmojiIconButton(onClick = onPermanentDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = stringResource(R.string.common_permanent),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** 时间戳转“yyyy-MM-dd HH:mm”格式字符串。 */
private fun formatTime(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
