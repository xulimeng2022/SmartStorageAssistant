package com.example.smartstorage.presentation.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.presentation.common.AnimatedButton
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.TimeoutDialog
import com.example.smartstorage.presentation.theme.textColorStyle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.smartstorage.data.remote.llm.ParsedItem
import java.io.File

/**
 * 添加/编辑物品路由：连接 ViewModel 与纯 UI，保存成功后返回上一页。
 *
 * @param item 传入表示编辑模式，用现有数据填充表单；为 null 表示新增。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemRoute(
    viewModel: AddItemViewModel = hiltViewModel(),
    item: Item? = null,
    onBack: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val parseState by viewModel.parseState.collectAsStateWithLifecycle()
    val textColorConfig by viewModel.textColorConfig.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val duplicateCheckState by viewModel.duplicateCheckState.collectAsStateWithLifecycle()
    val timeoutDialog by viewModel.timeoutDialog.collectAsStateWithLifecycle()
    val batchItems by viewModel.batchItems.collectAsStateWithLifecycle()
    val batchSelected by viewModel.batchSelected.collectAsStateWithLifecycle()
    val showBatchDialog by viewModel.showBatchDialog.collectAsStateWithLifecycle()
    val batchDuplicateNames by viewModel.batchDuplicateNames.collectAsStateWithLifecycle()
    val batchDuplicatePending by viewModel.batchDuplicatePending.collectAsStateWithLifecycle()

    // 进入页面时先重置表单状态（避免复用上次保存结果导致闪屏），
    // 编辑模式再加载现有物品数据
    LaunchedEffect(item?.id) {
        viewModel.clearState()
        item?.let(viewModel::loadItem)
    }

    // 保存成功后自动返回首页
    LaunchedEffect(saveState) {
        if (saveState == SaveState.Success) {
            onBack()
        }
    }

    AddItemScreen(
        isEditing = item != null,
        name = editState.name,
        location = editState.location,
        description = editState.desc,
        saveState = saveState,
        voiceDescription = editState.voiceDescription,
        parseState = parseState,
        textColorConfig = textColorConfig,
        imagePaths = editState.currentImagePaths,
        hasChanges = hasChanges,
        duplicateCheckState = duplicateCheckState,
        onDuplicateDecision = viewModel::onDuplicateDecision,
        onNameChange = viewModel::onNameChange,
        onLocationChange = viewModel::onLocationChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onVoiceDescriptionChange = viewModel::onVoiceDescriptionChange,
        onParseDescription = viewModel::parseDescription,
        onImageUriSelected = viewModel::onImageUriSelected,
        onImageFileSelected = viewModel::onImageFileSelected,
        onRemoveImageAt = viewModel::removeImageAt,
        onSave = viewModel::save,
        timeoutDialog = timeoutDialog,
        onConsumeTimeout = viewModel::consumeTimeout,
        onGoToSettings = onGoToSettings,
        batchItems = batchItems,
        batchSelected = batchSelected,
        showBatchDialog = showBatchDialog,
        batchDuplicateNames = batchDuplicateNames,
        batchDuplicatePending = batchDuplicatePending,
        onBatchSelectionChange = viewModel::onBatchSelectionChange,
        onBatchDuplicateChoice = viewModel::onBatchDuplicateChoice,
        onConfirmBatchAdd = viewModel::confirmBatchAdd,
        onDismissBatchDialog = viewModel::dismissBatchDialog,
        onBack = onBack,
    )
}

/**
 * 添加/编辑物品表单界面：
 * 语音描述（手机键盘语音 + 大模型智能解析）+ 手动填写“物品名 / 存放地点 / 备注”+ 照片附件。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    isEditing: Boolean,
    name: String,
    location: String,
    description: String,
    saveState: SaveState,
    voiceDescription: String,
    parseState: LlmParseState,
    textColorConfig: TextColorConfig,
    imagePaths: List<String>,
    hasChanges: Boolean,
    duplicateCheckState: DuplicateCheckState?,
    onDuplicateDecision: (DuplicateDecision) -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVoiceDescriptionChange: (String) -> Unit,
    onParseDescription: () -> Unit,
    onImageUriSelected: (Uri) -> Unit,
    onImageFileSelected: (File) -> Unit,
    onRemoveImageAt: (Int) -> Unit,
    onSave: () -> Unit,
    timeoutDialog: Boolean,
    onConsumeTimeout: () -> Unit,
    onGoToSettings: () -> Unit,
    batchItems: List<ParsedItem>,
    batchSelected: Set<Int>,
    showBatchDialog: Boolean,
    batchDuplicateNames: Set<String>,
    batchDuplicatePending: BatchDuplicatePending?,
    onBatchSelectionChange: (Int, Boolean) -> Unit,
    onBatchDuplicateChoice: (BatchDuplicateChoice) -> Unit,
    onConfirmBatchAdd: () -> Unit,
    onDismissBatchDialog: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // 输入文字样式：使用全局文字颜色配置（纯色 / 渐变 / 默认跟随主题），实时生效
    val inputTextStyle = textColorStyle(
        config = textColorConfig,
        baseStyle = MaterialTheme.typography.bodyLarge,
        defaultColor = MaterialTheme.colorScheme.onSurface,
    )


    // 未保存修改退出确认弹窗
    var showDiscardDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = hasChanges && saveState != SaveState.Saving) {
        showDiscardDialog = true
    }

    // 图片来源弹窗与临时文件
    var showImageSheet by remember { mutableStateOf(false) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }

    // 拍照：目标 URI 经 FileProvider 暴露缓存文件
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            cameraTempFile?.let(onImageFileSelected)
        }
    }

    // 启动系统相机：目标文件写入缓存目录并经 FileProvider 暴露
    val launchCamera: () -> Unit = {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraTempFile = file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        takePictureLauncher.launch(uri)
    }

    // 相机权限申请
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(context, "未授予相机权限，无法拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 相册：Android 13+ 用 Photo Picker 一次多选；旧版本用系统选择器单张多次（均无需存储权限）
    val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(AddItemViewModel.MAX_IMAGES),
    ) { uris -> uris.forEach { onImageUriSelected(it) } }

    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(onImageUriSelected) }

    val pickFromGallery: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMultipleMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        } else {
            getContentLauncher.launch("image/*")
        }
    }

    val takePhoto: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏：返回 + 标题
        TopAppBar(
            title = { Text(if (isEditing) "编辑物品" else "添加物品") },
            navigationIcon = {
                EmojiIconButton(
                    onClick = {
                        // 有未保存修改时先弹确认框
                        if (hasChanges && saveState != SaveState.Saving) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            },
        )

        // 表单区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== 语音描述（手机键盘语音输入 + 大模型智能解析）=====
            Text(
                text = "🎙️ 语音录入：点击麦克风说话，AI 自动提取物品信息\n支持批量录入（如“红色的笔在柜子里，蓝色的笔在抽屉里”）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = voiceDescription,
                onValueChange = onVoiceDescriptionChange,
                label = { Text("口语描述") },
                textStyle = inputTextStyle,
                placeholder = { Text("例如：那个红色的、上次去日本买的杯子放在橱柜第二层") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
            )
            Button(
                onClick = onParseDescription,
                enabled = parseState != LlmParseState.Parsing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                if (parseState == LlmParseState.Parsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("解析中…")
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("智能解析")
                }
            }
            // 解析状态提示
            when (parseState) {
                is LlmParseState.Error -> Text(
                    text = parseState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                LlmParseState.Success -> Text(
                    text = "解析完成，已自动填入下方表单，可手动微调",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                else -> Unit
            }

            // ===== 物品名 =====
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("物品名 *") },
                textStyle = inputTextStyle,
                placeholder = { Text("例如：充电器") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 存放地点
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("存放地点") },
                textStyle = inputTextStyle,
                placeholder = { Text("例如：客厅抽屉第二层") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 备注
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("备注") },
                textStyle = inputTextStyle,
                placeholder = { Text("补充物品的详细信息…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )

            // ===== 照片附件（最多 9 张）=====
            Text(
                text = "照片附件（辅助记忆，最多 ${AddItemViewModel.MAX_IMAGES} 张）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 3 列网格：已选图片 + 占位“添加”块
            val cellPaths: List<String?> = imagePaths +
                if (imagePaths.size < AddItemViewModel.MAX_IMAGES) listOf(null) else emptyList()
            cellPaths.chunked(3).forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCells.forEach { path ->
                        if (path == null) {
                            // 占位“添加”块：点击弹出图片来源选择
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showImageSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "添加照片",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        } else {
                            // 已选缩略图 + 右上角删除按钮
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "物品照片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                // 删除单张：仅从工作副本移除，文件在保存时才真正删除
                                EmojiIconButton(
                                    onClick = { onRemoveImageAt(imagePaths.indexOf(path)) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "删除照片",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                    // 补齐该行剩余列，保持间距一致
                    repeat(3 - rowCells.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 保存失败提示
            if (saveState is SaveState.Error) {
                Text(
                    text = saveState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // 保存按钮（立体按压 + Emoji 彩蛋）
            AnimatedButton(
                onClick = onSave,
                enabled = saveState != SaveState.Saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (saveState == SaveState.Saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(if (isEditing) "保存修改" else "保存")
                }
            }
        }
    }

    // 图片来源底部弹窗
    if (showImageSheet) {
        ModalBottomSheet(onDismissRequest = { showImageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "选择图片来源",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable {
                        showImageSheet = false
                        takePhoto()
                    },
                )
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier.clickable {
                        showImageSheet = false
                        pickFromGallery()
                    },
                )
            }
        }
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

    // 免费模式解析超时弹窗
    if (timeoutDialog) {
        TimeoutDialog(
            onGoToSettings = onGoToSettings,
            onLater = onConsumeTimeout,
        )
    }

    // 重复物品确认对话框：保存时发现同名旧记录，让用户选择更新/新建/取消
    duplicateCheckState?.let { dup ->
        AlertDialog(
            onDismissRequest = { onDuplicateDecision(DuplicateDecision.CANCEL) },
            title = { Text("物品已存在") },
            text = {
                Text(
                    "您已有一个名为“${dup.existingItem.name}”的物品，当前存放在" +
                        "“${dup.existingItem.location.ifBlank { "未填写" }}”。是否将其更新为新位置，还是新建一条记录？",
                )
            },
            confirmButton = {
                TextButton(onClick = { onDuplicateDecision(DuplicateDecision.UPDATE_EXISTING) }) {
                    Text("更新旧记录")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDuplicateDecision(DuplicateDecision.INSERT_NEW) }) {
                        Text("新建记录")
                    }
                    TextButton(onClick = { onDuplicateDecision(DuplicateDecision.CANCEL) }) {
                        Text("取消")
                    }
                }
            },
        )
    }

    // AI 批量解析确认 BottomSheet：多选物品 + 重名冲突处理 + 批量添加
    if (showBatchDialog) {
        ModalBottomSheet(onDismissRequest = onDismissBatchDialog) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📦 批量添加物品", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "检测到 ${batchItems.size} 件物品，请确认或编辑后批量添加\n（重复物品将在添加时逐一询问）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 物品列表：Checkbox 多选 + 名称 / 地点 + 重名标记
                batchItems.forEachIndexed { index, item ->
                    val checked = index in batchSelected
                    val isDuplicate = item.name.trim() in batchDuplicateNames
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBatchSelectionChange(index, !checked) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onBatchSelectionChange(index, it) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge)
                            if (item.location.isNotBlank()) {
                                Text(
                                    text = item.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (isDuplicate) {
                            Text(
                                text = "⚠ 已存在",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 批量添加按钮（X 为勾选数量，未勾选时禁用）
                Button(
                    onClick = onConfirmBatchAdd,
                    enabled = batchSelected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("批量添加 ${batchSelected.size} 件物品")
                }
            }
        }
    }

    // 批量处理中重复物品弹窗：展示旧/新信息，让用户逐条选择（点外部/返回键 = 跳过此物品并继续）
    batchDuplicatePending?.let { pending ->
        AlertDialog(
            onDismissRequest = { onBatchDuplicateChoice(BatchDuplicateChoice.SKIP) },
            title = { Text("⚠️ 物品已存在") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "“${pending.newItem.name}” 已存在",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (pending.existingItem.location.isNotBlank()) {
                        Text(
                            text = "当前位置：${pending.existingItem.location}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (pending.newItem.location.isNotBlank()) {
                        Text(
                            text = "新位置：${pending.newItem.location}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onBatchDuplicateChoice(BatchDuplicateChoice.UPDATE_EXISTING) }) {
                    Text("更新旧记录")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onBatchDuplicateChoice(BatchDuplicateChoice.INSERT_NEW) }) {
                        Text("新建记录")
                    }
                    TextButton(onClick = { onBatchDuplicateChoice(BatchDuplicateChoice.SKIP) }) {
                        Text("跳过此物品")
                    }
                }
            },
        )
    }
}
