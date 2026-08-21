package com.example.smartstorage.presentation.detail

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.presentation.common.EmojiEffect
import com.example.smartstorage.presentation.common.EmojiIconButton
import java.io.File
import java.time.Instant
import java.time.ZoneId
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.presentation.theme.textColorStyle
import java.time.format.DateTimeFormatter

/**
 * 物品详情页：大图（点击全屏预览）+ 信息 + 编辑/删除入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: (Item) -> Unit,
) {
    val viewModel: ItemDetailViewModel = hiltViewModel()
    val item by viewModel.item.collectAsStateWithLifecycle()
    val addingPhoto by viewModel.addingPhoto.collectAsStateWithLifecycle()
    val textColorConfig by viewModel.textColorConfig.collectAsStateWithLifecycle()

    // 加载并订阅该物品（编辑返回后自动刷新）
    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 图片来源弹窗与临时文件（拍照/相册，与添加页一致）
    var showImageSheet by remember { mutableStateOf(false) }
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> if (success) cameraTempFile?.let(viewModel::addPhoto) }

    val launchCamera: () -> Unit = {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraTempFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera() else {
            Toast.makeText(context, "未授予相机权限，无法拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 相册：Android 13+ 与低版本均可一次多选（均无需存储权限）
    val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }

    // 低版本相册：一次多选（与 Android 13+ 体验一致，均无需存储权限）
    val getMultipleContentsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }

    val pickFromGallery: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMultipleMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        } else {
            getMultipleContentsLauncher.launch("image/*")
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

    val current = item
    if (current == null) {
        // 加载中或已被删除
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("物品详情") },
            navigationIcon = {
                EmojiIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            },
            actions = {
                EmojiIconButton(onClick = { onEdit(current) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                EmojiIconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 照片区：全部照片横向滚动展示，点击某张进入全屏预览
            if (current.imagePaths.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(current.imagePaths, key = { it }) { path ->
                        Box {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "物品照片",
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { fullscreenIndex = current.imagePaths.indexOf(path) },
                                contentScale = ContentScale.Crop,
                            )
                            // 删除该张照片（立即删文件并更新数据库；带 Emoji 彩蛋）
                            val photoEmojis = remember { mutableStateListOf<Long>() }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable {
                                        if (photoEmojis.size < 3) {
                                            photoEmojis.add(System.nanoTime())
                                        }
                                        viewModel.removePhoto(path)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "删除照片",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White,
                                )
                                photoEmojis.forEach { id ->
                                    Box(
                                        modifier = Modifier.matchParentSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        EmojiEffect(onFinished = { photoEmojis.remove(id) })
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showImageSheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击添加照片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 添加照片按钮（继续添加新照片；添加中显示进度圈并禁用，防重复点击）
            TextButton(
                onClick = { showImageSheet = true },
                enabled = !addingPhoto,
            ) {
                if (addingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在添加…")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加照片")
                }
            }

            // 物品名
            Text(
                text = current.name,
                style = textColorStyle(
                    config = textColorConfig,
                    baseStyle = MaterialTheme.typography.headlineSmall,
                    defaultColor = MaterialTheme.colorScheme.onSurface,
                ),
            )

            // 存放地点
            if (current.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = current.location,
                        style = textColorStyle(
                            config = textColorConfig,
                            baseStyle = MaterialTheme.typography.bodyLarge,
                            defaultColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            // 备注
            if (current.description.isNotBlank()) {
                Text(
                    text = current.description,
                    style = textColorStyle(
                        config = textColorConfig,
                        baseStyle = MaterialTheme.typography.bodyMedium,
                        defaultColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "创建时间：${formatTime(current.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "最后修改：${formatTime(current.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // 全屏预览（点击黑底关闭）
    fullscreenIndex?.let { index ->
        val path = current.imagePaths.getOrNull(index)
        if (path != null) {
            Dialog(
                onDismissRequest = { fullscreenIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { fullscreenIndex = null },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "物品照片大图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }

    // 图片来源底部弹窗（拍照 / 从相册选择）
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

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除物品") },
            text = { Text("确定要删除「${current.name}」吗？将移入回收站，可在设置页的回收站中恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onDeleted = onBack)
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

/** 时间戳转“yyyy-MM-dd HH:mm”格式字符串。 */
private fun formatTime(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
