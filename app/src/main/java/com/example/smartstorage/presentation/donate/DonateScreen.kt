package com.example.smartstorage.presentation.donate

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smartstorage.presentation.common.EmojiIconButton

/**
 * 捐赠页路由：连接导航回调与纯 UI。
 */
@Composable
fun DonateRoute(onBack: () -> Unit) {
    DonateScreen(onBack = onBack)
}

/** 收款码弹窗状态：图片资源 + 标题。 */
private data class QrDialogState(val imageRes: Int, val title: String)

/**
 * 赞助页：多档位金额选择 → 选择支付方式 → 展示对应收款码，扫码完成赞助。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
    // 当前选中的档位
    var selectedOption by remember { mutableStateOf<DonateOption?>(null) }
    // 是否显示“选择支付方式”弹窗
    var showPaymentChooser by remember { mutableStateOf(false) }
    // 当前展示的收款码（非 null 时显示收款码弹窗）
    var qrDialog by remember { mutableStateOf<QrDialogState?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部返回栏
        TopAppBar(
            title = { Text("赞助支持") },
            navigationIcon = {
                EmojiIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ===== 问候语 =====
            Text(
                text = "☕ 如果这个应用对你有帮助",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "欢迎请我喝杯咖啡，感谢支持！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ===== 金额档位：每行 2 个按钮的网格布局 =====
            val options = DonateOption.presetOptions()
            options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowOptions.forEach { option ->
                        DonateOptionCard(
                            option = option,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedOption = option
                                showPaymentChooser = true
                            },
                        )
                    }
                    // 奇数个档位时用占位补满一行
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            // ===== 底部提示 =====
            Text(
                text = "💡 点击金额后，请使用对应 App 扫码即可赞助",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    // ===== 支付方式选择弹窗 =====
    if (showPaymentChooser) {
        PaymentChooserDialog(
            onAlipaySelected = {
                showPaymentChooser = false
                selectedOption?.let { qrDialog = QrDialogState(it.alipayRes, "支付宝收款码") }
            },
            onWechatSelected = {
                showPaymentChooser = false
                selectedOption?.let { qrDialog = QrDialogState(it.wechatRes, "微信收款码") }
            },
            onDismiss = { showPaymentChooser = false },
        )
    }

    // ===== 收款码展示弹窗 =====
    qrDialog?.let { state ->
        QRCodeDialog(
            title = state.title,
            imageRes = state.imageRes,
            onDismiss = { qrDialog = null },
        )
    }
}

/** 单个金额档位卡片：第一行文案，第二行金额。 */
@Composable
private fun DonateOptionCard(
    option: DonateOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = option.displayText,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.amountText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 支付方式选择弹窗：支付宝（蓝）/ 微信（绿）/ 取消。 */
@Composable
private fun PaymentChooserDialog(
    onAlipaySelected: () -> Unit,
    onWechatSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择支付方式") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 支付宝（品牌蓝）
                Button(
                    onClick = onAlipaySelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF)),
                ) {
                    Text("支付宝", color = Color.White)
                }
                // 微信（品牌绿）
                Button(
                    onClick = onWechatSelected,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160)),
                ) {
                    Text("微信", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

/** 收款码展示弹窗：居中大图 + 扫码提示 + 保存到相册。 */
@Composable
private fun QRCodeDialog(
    title: String,
    imageRes: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    // 低版本（Android 12 及以下）保存到相册需要存储权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            saveImageToGallery(context, imageRes)
        } else {
            Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "收款码",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Text(
                    text = "请使用对应 App 扫码转账",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 保存到相册：Android 13+ 无需权限直接保存；低版本需 WRITE_EXTERNAL_STORAGE
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        saveImageToGallery(context, imageRes)
                    } else if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        saveImageToGallery(context, imageRes)
                    } else {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }) {
                    Text("📥 保存到相册")
                }
                TextButton(onClick = onDismiss) {
                    Text("我知道了")
                }
            }
        },
    )
}

/**
 * 保存收款码到系统相册（MediaStore）。
 * Android 10+ 写入自有媒体无需权限；Android 13+ 完全无需权限；低版本需 WRITE_EXTERNAL_STORAGE。
 */
private fun saveImageToGallery(context: Context, drawableRes: Int) {
    try {
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableRes) ?: return
        val displayName = "donate_qr_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        )
        if (uri != null) {
            val saved = context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
            bitmap.recycle()
            if (saved) {
                Toast.makeText(context, "✅ 已保存到相册，请打开支付宝/微信扫码", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            bitmap.recycle()
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
    }
}