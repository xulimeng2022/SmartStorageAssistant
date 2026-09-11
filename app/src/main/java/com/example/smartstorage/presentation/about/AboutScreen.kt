package com.example.smartstorage.presentation.about

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.smartstorage.R

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartstorage.BuildConfig
import com.example.smartstorage.presentation.common.EmojiIconButton
import com.example.smartstorage.presentation.common.resolve

/**
 * 关于页路由：连接 ViewModel 与纯 UI。
 */
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    onOpenDonate: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel(),
) {
    // 一次性消息（复制联系方式成功）：用界面 Context 按当前语言解析为 Toast
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { message ->
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_LONG).show()
        }
    }
    AboutScreen(
        versionName = BuildConfig.VERSION_NAME,
        contacts = viewModel.contacts,
        onBack = onBack,
        onOpenDonate = onOpenDonate,
    )
}

/**
 * 关于页：Logo 区 + 版权信息 + 联系方式列表（数据驱动，可扩展）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    contacts: List<ContactItem>,
    onBack: () -> Unit,
    onOpenDonate: () -> Unit = {},
) {
    var showChangelog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部返回栏
        TopAppBar(
            title = { Text(stringResource(R.string.about_title)) },
            navigationIcon = {
                EmojiIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.about_back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ===== 1. Logo 区域（App 图标占位，可替换为应用图标资源）=====
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = stringResource(R.string.about_icon),
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider(modifier = Modifier.fillMaxWidth())

            // ===== 2. 版权信息（居中，小字灰色）=====
            Text(
                text = stringResource(R.string.about_copyright),
                modifier = Modifier.padding(vertical = 16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Divider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // ===== 3. 联系方式列表（数据驱动）=====
            contacts.forEach { contact ->
                ContactRow(contact)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // ===== 3.5 更新记录入口 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showChangelog = true })
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.about_changelog),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.about_enter),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ===== 4. 赞助支持入口（跳转捐赠页）=====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDonate)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.about_donate),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.about_enter),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // 更新记录弹窗（v1.1.0 三语说明）
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text(stringResource(R.string.changelog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• " + stringResource(R.string.changelog_new_1))
                    Text("• " + stringResource(R.string.changelog_new_2))
                    Text("• " + stringResource(R.string.changelog_fix_1))
                    Text("• " + stringResource(R.string.changelog_fix_2))
                    Text("• " + stringResource(R.string.changelog_fix_3))
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

/** 单个联系方式行：[图标] 名称: 值 + 右侧箭头（跳转）或复制图标（微信）。 */
@Composable
private fun ContactRow(contact: ContactItem) {
    // 中文品牌/分类名按当前语言显示（QQ/GitHub 等专名保持不变）
    val displayLabel = stringResource(contact.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = contact.onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = contact.icon,
            contentDescription = displayLabel,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = contact.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (contact.copyable) Icons.Outlined.ContentCopy else Icons.Outlined.ChevronRight,
            contentDescription = if (contact.copyable) stringResource(R.string.about_copy) else stringResource(R.string.about_open),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
