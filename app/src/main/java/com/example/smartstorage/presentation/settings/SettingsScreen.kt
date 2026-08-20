package com.example.smartstorage.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartstorage.BuildConfig
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.LlmPreset
import com.example.smartstorage.presentation.common.AnimatedButton
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.SearchTipsDialog

/**
 * 设置页：按功能分组卡片化展示（通用设置 / AI 智能解析 / 数据管理 / 关于与支持）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTrash: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenDonate: () -> Unit = {},
    onBack: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val modelList by viewModel.modelList.collectAsStateWithLifecycle()
    val selectedTextColor by viewModel.selectedTextColor.collectAsStateWithLifecycle()
    val appearanceState by viewModel.appearanceState.collectAsStateWithLifecycle()

    // 输入文字样式：与应用内其他输入框保持一致（使用用户自定义的颜色）
    val inputTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = Color(selectedTextColor),
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // 帮助弹窗显示状态
    var showHelpDialog by remember { mutableStateOf(false) }

    // 首页“搜索小贴士”弹窗状态
    var showSearchTips by remember { mutableStateOf(false) }
    var doNotRemindTips by remember { mutableStateOf(false) }

    // 退出确认：有未保存修改时按系统返回键 → 弹窗
    var showDiscardDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    // 保存成功/提示消息 → Snackbar
    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSaveMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            CenterAlignedTopAppBar(title = { Text("设置") })

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                // 保持滚动位置：进入子页面（如赞助支持）返回后仍停留在原位置
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // ===== 分组一：通用设置（语言预留 / 输入文字颜色 / 搜索小贴士）=====
                        SettingsGroup(
                            title = "通用设置",
                            items = listOf(
                                // 语言：预留项（置灰，开发中）
                                SettingsItem(
                                    label = "语言",
                                    value = "开发中",
                                    icon = Icons.Filled.Language,
                                    enabled = false,
                                    onClick = {},
                                ),
                                // 输入文字颜色：外观选色行（附加内容常驻展开）
                                SettingsItem(
                                    label = "输入文字颜色",
                                    value = "自定义",
                                    icon = Icons.Filled.Palette,
                                    onClick = {},
                                    content = {
                                        // 外观选色区（与原“外观设置”功能一致）
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                                        ) {
                                            Text(
                                                text = "选择输入框文字颜色，与浅色提示文字区分（保存后立即生效）",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                colorOptions.forEach { option ->
                                                    val selected = selectedTextColor == option.argb
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(option.argb))
                                                                .border(
                                                                    width = 3.dp,
                                                                    color = if (selected) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    } else {
                                                                        Color.Transparent
                                                                    },
                                                                    shape = CircleShape,
                                                                )
                                                                .clickable { viewModel.selectTextColor(option.argb) },
                                                        )
                                                        Spacer(modifier = Modifier.size(4.dp))
                                                        Text(
                                                            text = option.name,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }
                                            if (appearanceState == AppearanceSaveState.Saved) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "文字颜色已应用 ✅",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    },
                                ),
                                // 搜索小贴士：查看 / 重新开启首页搜索提示
                                SettingsItem(
                                    label = "搜索小贴士",
                                    value = "查看搜索方法",
                                    icon = Icons.Outlined.Help,
                                    onClick = {
                                        doNotRemindTips = !viewModel.isSearchTipsEnabled()
                                        showSearchTips = true
                                    },
                                ),
                            ),
                        )

                        // ===== 分组二：AI 智能解析（模式切换 + 模型配置，抽屉动画）=====
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 分组标题
                            Text(
                                text = "AI 智能解析",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            // AI 配置卡片（保留原卡片内容，自定义配置区加抽屉动画）
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // 卡片头部：图标 + 标题 + 帮助按钮
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "AI 智能解析",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        // 帮助按钮（问号）：弹出配置说明
                                        EmojiIconButton(onClick = { showHelpDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Help,
                                                contentDescription = "配置帮助",
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    // 使用模式切换：免费模式（内置免费模型）/ 自定义模式
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        SegmentedButton(
                                            selected = mode == AiConfig.MODE_FREE,
                                            onClick = { viewModel.onModeSelect(AiConfig.MODE_FREE) },
                                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        ) {
                                            Text("免费模式")
                                        }
                                        SegmentedButton(
                                            selected = mode == AiConfig.MODE_CUSTOM,
                                            onClick = { viewModel.onModeSelect(AiConfig.MODE_CUSTOM) },
                                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        ) {
                                            Text("自定义模式")
                                        }
                                    }

                                    // 自定义配置区：抽屉式展开 / 收拢动画（仅自定义模式显示，隐藏时不占布局空间）
                                    AnimatedVisibility(
                                        visible = mode == AiConfig.MODE_CUSTOM,
                                        enter = expandVertically(
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing,
                                            ),
                                        ) + fadeIn(animationSpec = tween(300)),
                                        exit = shrinkVertically(
                                            animationSpec = tween(300),
                                        ) + fadeOut(animationSpec = tween(200)),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            // 预设按钮行（可横向滑动，容纳全部接入商）
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                LlmPreset.entries.forEach { preset ->
                                                    FilterChip(
                                                        selected = editState.presetType == preset.label,
                                                        onClick = { viewModel.onPresetSelect(preset) },
                                                        label = { Text(preset.label) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                        ),
                                                    )
                                                }
                                            }

                                            // Base URL 输入框
                                            OutlinedTextField(
                                                value = editState.baseUrl,
                                                onValueChange = viewModel::onBaseUrlChange,
                                                label = { Text("Base URL") },
                                                placeholder = { Text("https://api.deepseek.com/v1") },
                                                textStyle = inputTextStyle,
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            // 模型版本下拉选择：非“自定义”只读（只能从列表选）；“自定义”无预设列表、允许手动输入
                                            val isCustom = editState.presetType == LlmPreset.CUSTOM.label
                                            var modelExpanded by remember { mutableStateOf(false) }
                                            ExposedDropdownMenuBox(
                                                expanded = modelExpanded,
                                                onExpandedChange = { if (modelList.isNotEmpty()) modelExpanded = it },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                OutlinedTextField(
                                                    value = editState.modelName,
                                                    onValueChange = viewModel::onModelChange,
                                                    readOnly = !isCustom,
                                                    label = { Text("模型版本") },
                                                    placeholder = { Text("deepseek-chat") },
                                                    textStyle = inputTextStyle,
                                                    singleLine = true,
                                                    trailingIcon = {
                                                        if (modelList.isNotEmpty()) {
                                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor(
                                                            type = if (isCustom) {
                                                                MenuAnchorType.PrimaryEditable
                                                            } else {
                                                                MenuAnchorType.PrimaryNotEditable
                                                            },
                                                        ),
                                                )
                                                if (modelList.isNotEmpty()) {
                                                    ExposedDropdownMenu(
                                                        expanded = modelExpanded,
                                                        onDismissRequest = { modelExpanded = false },
                                                    ) {
                                                        modelList.forEach { model ->
                                                            DropdownMenuItem(
                                                                text = { Text(model) },
                                                                onClick = {
                                                                    viewModel.onModelChange(model)
                                                                    modelExpanded = false
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            // API Key 输入框（加密存储，支持显示 / 隐藏）
                                            OutlinedTextField(
                                                value = editState.apiKey,
                                                onValueChange = viewModel::onApiKeyChange,
                                                label = { Text("API Key") },
                                                textStyle = inputTextStyle,
                                                singleLine = true,
                                                visualTransformation = if (editState.apiKeyVisible) {
                                                    VisualTransformation.None
                                                } else {
                                                    PasswordVisualTransformation()
                                                },
                                                trailingIcon = {
                                                    EmojiIconButton(onClick = viewModel::toggleApiKeyVisible) {
                                                        Icon(
                                                            imageVector = if (editState.apiKeyVisible) {
                                                                Icons.Filled.VisibilityOff
                                                            } else {
                                                                Icons.Filled.Visibility
                                                            },
                                                            contentDescription = if (editState.apiKeyVisible) {
                                                                "隐藏 API Key"
                                                            } else {
                                                                "显示 API Key"
                                                            },
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )

                                            // 保存按钮（立体按压 + Emoji 彩蛋）
                                            AnimatedButton(
                                                onClick = viewModel::saveAiConfig,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp),
                                            ) {
                                                Text("保存配置")
                                            }
                                        }
                                    }

                                    // 免费模式：内置免费模型，无需任何配置（无动画，仅免费模式显示）
                                    if (mode == AiConfig.MODE_FREE) {
                                        Text(
                                            text = "✅ 当前使用：${FreeModel.LABEL}（完全免费）",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "无需配置，即开即用。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // ===== 分组三：数据管理（回收站 / 导出数据预留）=====
                        SettingsGroup(
                            title = "数据管理",
                            items = listOf(
                                // 回收站：进入回收站页面
                                SettingsItem(
                                    label = "回收站",
                                    value = "查看被删除的物品，可恢复或永久删除",
                                    icon = Icons.Filled.DeleteSweep,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    onClick = onOpenTrash,
                                ),
                                // 导出数据：预留项（置灰，开发中）
                                SettingsItem(
                                    label = "导出数据",
                                    value = "开发中",
                                    icon = Icons.Outlined.FileDownload,
                                    enabled = false,
                                    onClick = {},
                                ),
                            ),
                        )

                        // ===== 分组四：关于与支持（关于 / 赞助支持 / 更多功能占位）=====
                        SettingsGroup(
                            title = "关于与支持",
                            items = listOf(
                                // 关于：进入关于页
                                SettingsItem(
                                    label = "关于智能收纳助手",
                                    value = "版本 ${BuildConfig.VERSION_NAME}",
                                    icon = Icons.Filled.Info,
                                    onClick = onOpenAbout,
                                ),
                                // 赞助支持：进入捐赠页
                                SettingsItem(
                                    label = "☕ 赞助支持",
                                    value = "请开发者喝杯咖啡，支持项目持续维护",
                                    icon = Icons.Outlined.Favorite,
                                    onClick = onOpenDonate,
                                ),
                                // 更多功能占位（保留）
                                SettingsItem(
                                    label = "更多功能开发中…",
                                    icon = Icons.Filled.Settings,
                                    enabled = false,
                                    onClick = {},
                                ),
                            ),
                        )
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

    // 首页“搜索小贴士”弹窗（勾选“不再提示”= 关闭首页提示；取消勾选 = 重新开启）
    if (showSearchTips) {
        SearchTipsDialog(
            doNotRemind = doNotRemindTips,
            onDoNotRemindChange = { doNotRemindTips = it },
            onConfirm = {
                showSearchTips = false
                viewModel.setSearchTipsEnabled(!doNotRemindTips)
            },
        )
    }

    // 帮助弹窗：说明如何配置 AI 智能解析
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("如何配置 AI 智能解析？") },
            text = {
                Text(
                    "0. 免费模式使用内置硅基流动 Qwen2.5-7B-Instruct，无需任何配置；自定义模式可填写自己的接入商与 API Key。\n\n" +
                        "1. 点击上方预设按钮（如 DeepSeek、OpenAI 等），会自动填入该供应商的接口地址与默认模型版本。\n\n" +
                        "2. 可在“模型版本”下拉框中选择具体模型；每个预设的 API Key 独立保存，切换预设会自动恢复对应的 Key。\n\n" +
                        "3. 填写该供应商的 API Key 后，点击“保存配置”。\n\n" +
                        "4. 配置完成后，在“添加物品”页的口语描述框输入或语音录入，即可自动解析为物品名、地点和备注。",
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }

    // 未保存修改退出确认
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("确定要放弃已修改的内容吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                ) {
                    Text("放弃修改", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("继续编辑")
                }
            },
        )
    }
    }
}

/** 输入文字颜色可选列表。 */
private val colorOptions = listOf(
    TextColorOption("深黑", 0xFF111111.toInt()),
    TextColorOption("深蓝", 0xFF0D47A1.toInt()),
    TextColorOption("深灰", 0xFF424242.toInt()),
    TextColorOption("墨绿", 0xFF1B5E20.toInt()),
    TextColorOption("深棕", 0xFF4E342E.toInt()),
)

/**
 * 设置项数据模型：单行设置项（图标 + 标题 + 可选辅助文字 + 右侧箭头）。
 *
 * @param label 标题文字
 * @param value 右侧辅助文字（可为空，如“开发中”）
 * @param icon 左侧图标
 * @param iconTint 图标颜色（null 时用 onSurfaceVariant）
 * @param enabled 是否可点击（false 表示置灰预留项，点击不生效）
 * @param onClick 点击回调
 * @param content 附加内容（渲染在该项主行下方，如输入文字颜色的选色行）
 */
private data class SettingsItem(
    val label: String,
    val value: String? = null,
    val icon: ImageVector,
    val iconTint: Color? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val content: (@Composable () -> Unit)? = null,
)

/**
 * 设置分组：分组标题 + 圆角卡片，组内设置项之间用分隔线分隔。
 *
 * @param title 分组标题（13sp、灰色、Medium，与卡片间距 4dp）
 * @param items 组内设置项列表
 */
@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 分组标题：小字、灰色、Medium，与卡片间距 4dp
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        // 分组卡片：圆角 12dp，背景 surface
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsListItem(
                        item = item,
                        // 最后一项不显示分隔线
                        showDivider = index < items.lastIndex,
                    )
                }
            }
        }
    }
}

/**
 * 设置列表项：一行（图标 + 标题 + 辅助文字 + 箭头）+ 可选附加内容 + 分隔线。
 *
 * @param item 设置项数据
 * @param showDivider 是否在底部显示分隔线（最后一项不显示）
 */
@Composable
private fun SettingsListItem(
    item: SettingsItem,
    showDivider: Boolean,
) {
    Column {
        // 主行：图标 + 标题 + 辅助文字 + 右侧箭头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = item.enabled, onClick = item.onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            // 右侧辅助文字（如版本号 / 开发中）
            if (item.value != null) {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 右侧箭头（仅可点击项显示）
            if (item.enabled) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 附加内容（如输入文字颜色的选色行）
        item.content?.invoke()
        // 分隔线（缩进与文字对齐；最后一项不显示）
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
            )
        }
    }
}
