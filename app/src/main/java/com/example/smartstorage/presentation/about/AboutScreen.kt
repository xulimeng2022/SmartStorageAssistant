package com.example.smartstorage.presentation.about

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

/**
 * 关于页路由：连接 ViewModel 与纯 UI。
 */
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    onOpenDonate: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel(),
) {
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
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部返回栏
        TopAppBar(
            title = { Text("关于") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ===== 1. Logo 区域（App 图标占位，可替换为应用图标资源）=====
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = "应用图标",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "智能收纳助手",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "版本 $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider(modifier = Modifier.fillMaxWidth())

            // ===== 2. 版权信息（居中，小字灰色）=====
            Text(
                text = "© 2026 徐力萌",
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
                    text = "☕ 赞助支持",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "进入",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 单个联系方式行：[图标] 名称: 值 + 右侧箭头（跳转）或复制图标（微信）。 */
@Composable
private fun ContactRow(contact: ContactItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = contact.onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = contact.icon,
            contentDescription = contact.label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.label,
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
            contentDescription = if (contact.copyable) "复制" else "跳转",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}