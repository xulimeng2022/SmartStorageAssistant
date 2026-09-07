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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.delay
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.smartstorage.BuildConfig
import com.example.smartstorage.R
import com.example.smartstorage.data.local.prefs.AppLanguage
import com.example.smartstorage.data.local.prefs.AiConfig
import com.example.smartstorage.data.local.prefs.FreeModel
import com.example.smartstorage.data.local.prefs.LlmPreset
import com.example.smartstorage.presentation.common.AnimatedButton
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.SearchTipsDialog
import com.example.smartstorage.presentation.theme.textColorStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import com.example.smartstorage.data.local.backup.ImportMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页：按功能分组卡片化展示（通用设置 / 外观设置 / AI 智能解析 / 数据管理 / 关于与支持）。
 *
 * LazyColumn 每个分组独立为一个 item（外观设置为第 2 个 item），
 * 便于「文字颜色」抽屉展开时通过 animateScrollToItem 自动滚动定位。
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
    // 外观设置：主题模式 + 全局文字颜色（实时流，修改后全局立即生效）
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val textColorConfig by viewModel.textColorConfig.collectAsStateWithLifecycle()
    // 数据备份与恢复状态
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val showExportNameDialog by viewModel.showExportNameDialog.collectAsStateWithLifecycle()
    val defaultExportName by viewModel.defaultExportName.collectAsStateWithLifecycle()
    val pendingImportInfo by viewModel.pendingImportInfo.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()

    // 导出：系统「保存位置」选择器（CreateDocument，仅限 zip）
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> viewModel.onExportNameConfirmed(viewModel.defaultExportName.value, uri) }
    // 导入：系统文件选择器（OpenDocument，选择 zip 备份文件）
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onImportFileSelected) }

    // 输入文字样式：使用全局文字颜色配置（默认类型跟随主题），设置页各输入框实时生效
    val inputTextStyle = textColorStyle(
        config = textColorConfig,
        baseStyle = MaterialTheme.typography.bodyLarge,
        defaultColor = MaterialTheme.colorScheme.onSurface,
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // 备份结果消息 → Snackbar
    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeBackupMessage()
        }
    }

    // 帮助弹窗显示状态
    var showHelpDialog by remember { mutableStateOf(false) }

    // 数据管理「备份与恢复教程」弹窗状态
    var showDataHelpDialog by remember { mutableStateOf(false) }

    // 首页「搜索小贴士」弹窗状态
    var showSearchTips by remember { mutableStateOf(false) }
    var doNotRemindTips by remember { mutableStateOf(false) }

    // 「去配置 API」进入：自动滚动定位到「AI 智能解析」卡片（LazyColumn 第 3 项，index=2）
    val autoOpenAiConfig by viewModel.autoOpenAiConfig.collectAsStateWithLifecycle()
    LaunchedEffect(autoOpenAiConfig) {
        if (autoOpenAiConfig) {
            viewModel.consumeAutoOpenAiConfig()
            delay(120)
            listState.animateScrollToItem(2)
        }
    }

    // 语言选择弹窗状态
    var showLanguageDialog by remember { mutableStateOf(false) }

    // 退出确认：有未保存修改时按系统返回键先弹窗
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
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.settings_title)) })

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                // ===== 分组 0：通用设置（语言预留 / 搜索小贴士）=====
                item(key = "general") {
                    SettingsGroup(
                        title = stringResource(R.string.settings_general),
                        items = listOf(
                            // 语言：跟随系统 / 简体中文 / 繁體中文 / English
                            SettingsItem(
                                label = stringResource(R.string.settings_language),
                                value = currentLanguageLabel(),
                                icon = Icons.Filled.Language,
                                enabled = true,
                                onClick = { showLanguageDialog = true },
                            ),
                            // 搜索小贴士：查看 / 重新开启首页搜索提示
                            SettingsItem(
                                label = stringResource(R.string.settings_searchtips),
                                value = stringResource(R.string.settings_searchtips_value),
                                icon = Icons.Outlined.Help,
                                onClick = {
                                    doNotRemindTips = !viewModel.isSearchTipsEnabled()
                                    showSearchTips = true
                                },
                            ),
                        ),
                    )
                }

                // ===== 分组 1：外观设置（深色模式 + 文字颜色抽屉）=====
                item(key = "appearance") {
                    AppearanceSettingsGroup(
                        themeMode = themeMode,
                        textColorConfig = textColorConfig,
                        onThemeModeSelect = viewModel::onThemeModeSelect,
                        onTextColorConfigChange = viewModel::onTextColorConfigChange,
                        listState = listState,
                    )
                }

                // ===== 分组 2：AI 智能解析（模式切换 + 模型配置，抽屉动画）=====
                item(key = "ai") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 分组标题
                        Text(
                            text = stringResource(R.string.ai_group_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        // AI 配置卡片（自定义配置区加抽屉动画）
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
                                        text = stringResource(R.string.ai_group_title),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    // 帮助按钮（问号）：弹出配置说明
                                    EmojiIconButton(onClick = { showHelpDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Help,
                                            contentDescription = stringResource(R.string.help_cd_config),
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
                                        Text(stringResource(R.string.free_mode))
                                    }
                                    SegmentedButton(
                                        selected = mode == AiConfig.MODE_CUSTOM,
                                        onClick = { viewModel.onModeSelect(AiConfig.MODE_CUSTOM) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    ) {
                                        Text(stringResource(R.string.custom_mode))
                                    }
                                }

                                // 自定义配置区：抽屉式展开 / 收拢动画（仅自定义模式显示）
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
                                        // 模型版本下拉选择：非「自定义」只读；「自定义」允许手动输入
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
                                                label = { Text(stringResource(R.string.model_version)) },
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
                                                            stringResource(R.string.hide_api_key)
                                                        } else {
                                                            stringResource(R.string.show_api_key)
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
                                            Text(stringResource(R.string.save_config))
                                        }
                                    }
                                }

                                // 免费模式：内置免费模型，无需任何配置
                                if (mode == AiConfig.MODE_FREE) {
                                    Text(
                                        text = stringResource(R.string.free_current, FreeModel.LABEL),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = stringResource(R.string.free_tip),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // ===== 分组 3：数据管理（回收站 / 导出数据预留）=====
                item(key = "data") {
                    SettingsGroup(
                        title = stringResource(R.string.data_group),
                        trailing = {
                            // 数据管理模块右上角帮助按钮：弹出备份与恢复教程
                            EmojiIconButton(onClick = { showDataHelpDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Help,
                                    contentDescription = stringResource(R.string.backup_help_cd),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        items = listOf(
                            // 回收站：进入回收站页面
                            SettingsItem(
                                label = stringResource(R.string.trash),
                                value = stringResource(R.string.trash_desc),
                                icon = Icons.Filled.DeleteSweep,
                                iconTint = MaterialTheme.colorScheme.error,
                                onClick = onOpenTrash,
                            ),
                            // 导出数据：导出所有物品与图片到 ZIP 备份文件
                            SettingsItem(
                                label = stringResource(R.string.export_data),
                                value = stringResource(R.string.export_desc),
                                icon = Icons.Outlined.FileDownload,
                                onClick = { viewModel.onExportClick() },
                            ),
                            // 导入数据：从备份文件恢复数据
                            SettingsItem(
                                label = stringResource(R.string.import_data),
                                value = stringResource(R.string.import_desc),
                                icon = Icons.Outlined.FileUpload,
                                onClick = { openDocumentLauncher.launch(arrayOf("application/zip")) },
                            ),
                        ),
                    )
                }

                // ===== 分组 4：关于与支持（关于 / 赞助支持 / 更多功能占位）=====
                item(key = "about") {
                    SettingsGroup(
                        title = stringResource(R.string.about_group),
                        items = listOf(
                            // 关于：进入关于页
                            SettingsItem(
                                label = stringResource(R.string.about_app),
                                value = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                                icon = Icons.Filled.Info,
                                onClick = onOpenAbout,
                            ),
                            // 赞助支持：进入捐赠页
                            SettingsItem(
                                label = stringResource(R.string.donate_entry),
                                value = stringResource(R.string.donate_desc),
                                icon = Icons.Outlined.Favorite,
                                onClick = onOpenDonate,
                            ),
                            // 更多功能占位（保留）
                            SettingsItem(
                                label = stringResource(R.string.more_coming),
                                icon = Icons.Filled.Settings,
                                enabled = false,
                                onClick = {},
                            ),
                        ),
                    )
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // 首页「搜索小贴士」弹窗（勾选「不再提示」关闭首页提示；取消勾选 = 重新开启）
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
                    "0. 免费模式内置硅基流动 Qwen2.5-7B-Instruct（完全免费）：免费模式内置免费模型，无需任何配置；若模型暂时不可用，可切换到自定义模式并填写自己的接入商与 API Key 后重试。自定义模式可填写任意 OpenAI 兼容接口。\n\n" +
                        "1. 点击上方预设按钮（如 DeepSeek、OpenAI 等），会自动填入该供应商的接口地址与默认模型版本。\n\n" +
                        "2. 可在「模型版本」下拉框中选择具体模型；每个预设的 API Key 独立保存，切换预设会自动恢复对应的 Key。\n\n" +
                        "3. 填写该供应商的 API Key 后，点击「保存配置」。\n\n" +
                        "4. 配置完成后，在「添加物品」页的口语描述框输入或语音录入，即可自动解析为物品名、地点和备注。" +
                        "5. 支持批量识别：一次输入多条描述（用逗号、句号或分号分隔），AI 会自动拆分为多条物品并逐个确认添加。",
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }

    // 数据管理「备份与恢复教程」弹窗
    if (showDataHelpDialog) {
        AlertDialog(
            onDismissRequest = { showDataHelpDialog = false },
            title = { Text("📦 数据备份与恢复教程") },
            text = {
                Text(
                    "📤 导出数据：\n点击「导出数据」，选择保存位置，即可将所有物品（含图片）备份为一个 ZIP 文件。\n\n" +
                        "📥 导入数据：\n点击「导入数据」，选择之前备份的 ZIP 文件，选择「覆盖」或「合并」模式即可恢复数据。\n\n" +
                        "💡 建议定期导出备份，避免数据丢失！",
                )
            },
            confirmButton = {
                TextButton(onClick = { showDataHelpDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }

    // 未保存修改退出确认
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.discard_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.discard_cancel))
                }
            },
        )
    }

    // 导出文件名输入对话框：确认后调用系统保存位置选择器
    if (showExportNameDialog) {
        var fileName by remember { mutableStateOf(defaultExportName) }
        AlertDialog(
            onDismissRequest = viewModel::onExportNameDismiss,
            title = { Text("导出数据") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请输入备份文件名：", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        textStyle = inputTextStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = fileName.trim().ifBlank { defaultExportName }
                        createDocumentLauncher.launch(name)
                    },
                ) {
                    Text(stringResource(R.string.export_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onExportNameDismiss) {
                    Text("取消")
                }
            },
        )
    }

    // 导入确认对话框：展示备份信息 + 选择导入模式（覆盖 / 合并）
    pendingImportInfo?.let { info ->
        AlertDialog(
            onDismissRequest = viewModel::onImportDismiss,
            title = { Text("导入数据") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_export_time, formatBackupTime(info.exportTime)), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.backup_item_count, info.itemCount), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.choose_import_mode), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 合并：保留现有数据，按名称去重，跳过重复物品
                        TextButton(onClick = { viewModel.onImportModeSelected(ImportMode.MERGE) }) {
                            Text(stringResource(R.string.import_mode_merge))
                        }
                        // 覆盖：清空现有数据与图片后导入
                        TextButton(onClick = { viewModel.onImportModeSelected(ImportMode.OVERWRITE) }) {
                            Text(stringResource(R.string.import_mode_overwrite), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onImportDismiss) {
                    Text("取消")
                }
            },
        )
    }

    // 导出 / 导入进度对话框（阻止用户操作，完成后自动关闭）
    if (backupBusy) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.please_wait)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.processing_backup), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {},
        )
    }

    // 语言选择弹窗：选择后持久化并重建 Activity 立即生效（语言与深浅主题互不覆盖）
    if (showLanguageDialog) {
        val context = LocalContext.current
        val current = AppLanguage.getCode(context)
        val options = listOf(
            "" to stringResource(R.string.lang_follow_system),
            "zh" to stringResource(R.string.lang_zh),
            "zh-rTW" to stringResource(R.string.lang_tw),
            "en" to stringResource(R.string.lang_en),
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.lang_dialog_title)) },
            text = {
                Column {
                    options.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    if (code != current) {
                                        AppLanguage.setCode(context, code)
                                        (context as? Activity)?.recreate()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (code == current) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (code == current) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** 时间戳转「yyyy-MM-dd HH:mm」格式（导入确认对话框展示导出时间） */
private fun formatBackupTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

/**
 * 设置项数据模型：单行设置项（图标 + 标题 + 可选辅助文字 + 右侧箭头）。
 *
 * @param label 标题文字
 * @param value 右侧辅助文字（可为空，如「开发中」）
 * @param icon 左侧图标
 * @param iconTint 图标颜色（null 时用 onSurfaceVariant）
 * @param enabled 是否可点击（false 表示置灰预留项，点击不生效）
 * @param onClick 点击回调
 * @param content 附加内容（渲染在该项主行下方，如外观设置的选色/抽屉内容）
 * @param trailingIcon 自定义右侧图标（null 时按 enabled 显示默认箭头；用于文字颜色抽屉的旋转箭头）
 */
internal data class SettingsItem(
    val label: String,
    val value: String? = null,
    val icon: ImageVector,
    val iconTint: Color? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val content: (@Composable () -> Unit)? = null,
    val trailingIcon: (@Composable () -> Unit)? = null,
)

/**
 * 设置分组：分组标题 + 圆角卡片，组内设置项之间用分隔线分隔。
 *
 * @param title 分组标题（13sp、灰色、Medium，与卡片间距 4dp）
 * @param items 组内设置项列表
 */
@Composable
internal fun SettingsGroup(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 分组标题行：标题占满剩余宽度，右侧可放辅助按钮（如数据管理的帮助按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // 右侧辅助按钮（trailing，如数据管理的帮助按钮）
            trailing?.invoke()
        }
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
internal fun SettingsListItem(
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
            // 右侧图标：优先使用自定义 trailingIcon（如抽屉旋转箭头），否则可点击项显示默认箭头
            if (item.trailingIcon != null) {
                item.trailingIcon()
            } else if (item.enabled) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.about_enter),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // 附加内容（如外观设置的选色/抽屉内容）
        item.content?.invoke()
        // 分隔线（缩进与文字对齐；最后一项不显示）
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
            )
        }
    }
}

/** 当前语言显示名（跟随系统 / 简体中文 / 繁體中文 / English）。 */
@Composable
private fun currentLanguageLabel(): String = when (AppLanguage.getCode(LocalContext.current)) {
    "zh" -> stringResource(R.string.lang_zh)
    "zh-rTW" -> stringResource(R.string.lang_tw)
    "en" -> stringResource(R.string.lang_en)
    else -> stringResource(R.string.lang_follow_system)
}
