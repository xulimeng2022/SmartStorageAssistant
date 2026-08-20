package com.example.smartstorage.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.presentation.common.EmojiEffect
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.SearchTipsDialog
import com.example.smartstorage.presentation.common.TimeoutDialog
import java.io.File

/**
 * 首页路由：连接 ViewModel 与纯 UI。
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onAddClick: () -> Unit,
    onEditClick: (Item) -> Unit,
    onItemClick: (Item) -> Unit,
    onGoToSettings: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val undoEvent by viewModel.undoEvent.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val parseState by viewModel.parseState.collectAsStateWithLifecycle()
    val timeoutDialog by viewModel.timeoutDialog.collectAsStateWithLifecycle()

    HomeScreen(
        items = items,
        undoEvent = undoEvent,
        searchQuery = searchQuery,
        parseState = parseState,
        timeoutDialog = timeoutDialog,
        onUndoDelete = viewModel::undoDelete,
        onConsumeUndo = viewModel::consumeUndo,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onAiParse = viewModel::onAiParse,
        onConsumeTimeout = viewModel::consumeTimeout,
        isSearchTipsEnabled = viewModel::isSearchTipsEnabled,
        onSetSearchTipsEnabled = viewModel::setSearchTipsEnabled,
        onGoToSettings = onGoToSettings,
        onAddClick = onAddClick,
        onItemClick = onItemClick,
        onDeleteClick = viewModel::delete,
    )
}

/**
 * 首页（物品清单）：
 * 根容器 Column：标题栏（固定）→ 搜索栏（固定高度 72dp）→ 物品列表（weight(1f) 占剩余空间）。
 * 交互按钮（麦克风、智能解析、删除、空状态添加）均带 Emoji 彩蛋；搜索框本身不触发。
 */
@Composable
fun HomeScreen(
    items: List<Item>,
    undoEvent: Item?,
    searchQuery: String,
    parseState: SearchParseState,
    onUndoDelete: () -> Unit,
    onConsumeUndo: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAiParse: () -> Unit,
    isSearchTipsEnabled: () -> Boolean,
    onSetSearchTipsEnabled: (Boolean) -> Unit,
    timeoutDialog: Boolean,
    onConsumeTimeout: () -> Unit,
    onGoToSettings: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Item) -> Unit,
    onDeleteClick: (Item) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 搜索框：聚焦与键盘控制（点麦克风聚焦后调用键盘语音）
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 搜索小贴士弹窗状态
    var showSearchTips by remember { mutableStateOf(false) }
    var tipsShownThisLaunch by rememberSaveable { mutableStateOf(false) }
    var doNotRemindTips by remember { mutableStateOf(false) }

    // 首次聚焦搜索条时弹出“搜索小贴士”（未禁用且本启动未弹过才弹）
    fun maybeShowSearchTips() {
        if (tipsShownThisLaunch || !isSearchTipsEnabled()) return
        tipsShownThisLaunch = true
        doNotRemindTips = !isSearchTipsEnabled()
        showSearchTips = true
    }

    // 软删除成功 → Snackbar 提示（约 4 秒自动消失），可点击“撤销”立即恢复
    LaunchedEffect(undoEvent) {
        undoEvent?.let {
            val result = snackbarHostState.showSnackbar(
                message = "已移入回收站",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoDelete()
            }
            onConsumeUndo()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 1. 标题栏（固定高度）=====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "物品清单",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // ===== 2. 搜索栏（固定高度 72dp，水平一条）=====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 搜索输入框：weight(1f) 占满剩余宽度，单行固定高度，文字实时可见
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索物品...") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    // 清除按钮（输入非空时显示）
                    if (searchQuery.isNotEmpty()) {
                        EmojiIconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { if (it.isFocused) maybeShowSearchTips() },
            )

            // 麦克风按钮（固定 48dp，带 Emoji）：聚焦搜索框并调起键盘语音
            EmojiIconButton(
                onClick = {
                    searchFocusRequester.requestFocus()
                    keyboardController?.show()
                    Toast.makeText(
                        context,
                        "请用键盘语音说出要找的物品",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "语音搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 智能解析按钮（固定 48dp，带 Emoji）：提取搜索关键词并填入搜索框
            EmojiIconButton(
                onClick = {
                    if (searchQuery.isBlank()) {
                        Toast.makeText(context, "请先输入搜索内容", Toast.LENGTH_SHORT).show()
                    } else {
                        onAiParse()
                    }
                },
                modifier = Modifier.size(48.dp),
            ) {
                if (parseState == SearchParseState.Parsing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "智能解析",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // 解析失败提示（小字）
        if (parseState == SearchParseState.Error) {
            Text(
                text = "解析失败，已使用原文搜索",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ===== 3. 物品列表 / 空状态（占据剩余全部空间）=====
        if (items.isEmpty()) {
            EmptyState(onAddClick = onAddClick, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteClick(item) },
                    )
                }
            }
        }
        }

        // 底部 Snackbar（撤销提示）
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // 免费模式解析超时弹窗
    if (timeoutDialog) {
        TimeoutDialog(
            onGoToSettings = onGoToSettings,
            onLater = onConsumeTimeout,
        )
    }

    // 搜索小贴士弹窗（居中；勾选“不再提示”后写入设置，可到设置页重新开启）
    if (showSearchTips) {
        SearchTipsDialog(
            doNotRemind = doNotRemindTips,
            onDoNotRemindChange = { doNotRemindTips = it },
            onConfirm = {
                showSearchTips = false
                if (doNotRemindTips) onSetSearchTipsEnabled(false)
            },
        )
    }
}

/**
 * 物品卡片：左侧第一张缩略图（无图占位）+ 名称/地点 + 右侧删除按钮（带 Emoji）。
 * 点击整卡跳转详情页。
 */
@Composable
private fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 缩略图（第一张；无图显示占位图标）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                val thumbPath = item.imagePath
                if (!thumbPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(thumbPath),
                        contentDescription = "物品照片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 名称 + 存放地点
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.location.isNotBlank()) {
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 删除按钮（带 Emoji）：移入回收站
            EmojiIconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * 空状态：居中圆形“添加物品”按钮（仅文字，无图标；点击带 Emoji 彩蛋）。
 */
@Composable
private fun EmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emojis = remember { mutableStateListOf<Long>() }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // 圆形“添加物品”按钮（约 120dp，主题色背景，白字）
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    if (emojis.size < 3) {
                        emojis.add(System.nanoTime())
                    }
                    onAddClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "添加物品",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        // Emoji 彩蛋层
        emojis.forEach { id ->
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmojiEffect(onFinished = { emojis.remove(id) })
            }
        }
    }
}
