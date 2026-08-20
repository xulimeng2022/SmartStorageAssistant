package com.example.smartstorage.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.LlmPreset
import com.example.smartstorage.presentation.common.AnimatedButton
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.SearchTipsDialog

/**
 * 设置页：AI 智能解析配置（CC Switch 风格卡片）+ 外观设置 + 关于信息。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTrash: () -> Unit = {},
    onBack: () -> Unit = {},
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ===== AI 智能解析配置卡片 =====
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 卡片头部：图标 + 标题（图标与添加页“智能解析”保持一致，无任何“开启/关闭”状态文字）
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

                        if (mode == AiConfig.MODE_CUSTOM) {
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

                        // 输入区
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
                        } else {
                            // 免费模式：内置免费模型，无需任何配置
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

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // ===== 回收站入口 =====
                ListItem(
                    headlineContent = { Text("回收站") },
                    supportingContent = { Text("查看被删除的物品，可恢复或永久删除") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.clickable(onClick = onOpenTrash),
                )

                // ===== 搜索小贴士入口（可再次查看/重新开启首页提示）=====
                ListItem(
                    headlineContent = { Text("搜索小贴士") },
                    supportingContent = { Text("查看首页搜索方法提示") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable(onClick = {
                        doNotRemindTips = !viewModel.isSearchTipsEnabled()
                        showSearchTips = true
                    }),
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // ===== 外观设置：输入文字颜色 =====
                Text(
                    text = "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "选择输入框文字颜色，与浅色提示文字区分（保存后立即生效）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    Text(
                        text = "文字颜色已应用 ✅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // ===== 关于 =====
                ListItem(
                    headlineContent = { Text("关于智能收纳助手") },
                    supportingContent = { Text("版本 2.7.8") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                        )
                    },
                )

                // 更多功能占位
                ListItem(
                    headlineContent = { Text("更多功能开发中…") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                        )
                    },
                )
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
                    "0. 免费模式使用内置硅基流动 Qwen3.5-4B，无需任何配置；自定义模式可填写自己的接入商与 API Key。\n\n" +
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
