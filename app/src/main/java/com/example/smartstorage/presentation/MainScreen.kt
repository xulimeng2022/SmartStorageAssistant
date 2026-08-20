package com.example.smartstorage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartstorage.domain.model.Item
import com.example.smartstorage.presentation.about.AboutRoute
import com.example.smartstorage.presentation.add.AddItemRoute
import com.example.smartstorage.presentation.add.AddItemViewModel
import com.example.smartstorage.presentation.detail.ItemDetailScreen
import com.example.smartstorage.presentation.donate.DonateRoute
import com.example.smartstorage.presentation.home.HomeRoute
import com.example.smartstorage.presentation.common.EmojiEffect
import com.example.smartstorage.presentation.navigation.Screen
import com.example.smartstorage.presentation.settings.SettingsScreen
import com.example.smartstorage.presentation.settings.SettingsViewModel
import com.example.smartstorage.presentation.trash.TrashScreen

/**
 * 应用主界面：底部导航（首页 / 添加 / 设置）+ 页面容器。
 *
 * 底部导航使用自定义实现（固定高度 Row），不使用 Material3 NavigationBar，
 * 规避部分新机型上 NavigationBar 测量高度异常、把内容区挤为 0 的问题。
 *
 * 页面切换统一拦截：编辑页/设置页有未保存修改时，底部 Tab 切换先弹确认框。
 */
@Composable
fun MainScreen() {
    // 当前页面（简单状态切换）
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }

    // 当前正在编辑的物品（null 表示新增）
    var editingItem by remember { mutableStateOf<Item?>(null) }

    // 当前查看详情的物品
    var viewingItem by remember { mutableStateOf<Item?>(null) }

    // 添加/编辑页共用 ViewModel；设置页 ViewModel
    val addViewModel: AddItemViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // 设置页滚动状态（提升到 MainScreen，进入子页面返回后仍保持滚动位置）
    val settingsListState = rememberLazyListState()

    // 是否有未保存修改（用于拦截底部 Tab 切换）
    val addHasChanges by addViewModel.hasChanges.collectAsStateWithLifecycle()
    val settingsHasChanges by settingsViewModel.hasChanges.collectAsStateWithLifecycle()

    // 待确认的 Tab 切换目标（null 表示无）
    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // 底部导航点击触发的 Emoji 彩蛋（MainScreen 常驻，页面切换不被中断；最多 3 个并发）
    val navEmojis = remember { mutableStateListOf<Long>() }

    // 统一的“放弃修改”确认弹窗（底部 Tab 切换拦截用）
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingRoute = null
            },
            title = { Text("放弃修改？") },
            text = { Text("确定要放弃已修改的内容吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        val target = pendingRoute
                        pendingRoute = null
                        // 丢弃未保存修改（工作副本直接作废，不写数据库、不删文件）
                        if (currentRoute == Screen.Add.route || currentRoute == Screen.Edit.route) {
                            addViewModel.clearState()
                        }
                        // 离开编辑页时清空正在编辑的物品，避免残留状态导致再次确认
                        if (currentRoute == Screen.Edit.route) {
                            editingItem = null
                        }
                        if (target != null) {
                            if (target == Screen.Settings.route) {
                                settingsViewModel.refresh()
                            }
                            currentRoute = target
                        }
                    },
                ) {
                    Text("放弃修改", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        pendingRoute = null
                    },
                ) {
                    Text("继续编辑")
                }
            },
        )
    }

    // “去配置”：跳转设置页 AI 配置（丢弃添加/编辑页未保存内容）
    val goToSettings: () -> Unit = {
        addViewModel.clearState()
        settingsViewModel.refresh()
        currentRoute = Screen.Settings.route
    }

    // 全面屏返回/系统返回：非首页返回上一级（首页保持默认退出；页面自身的 BackHandler 优先）
    BackHandler(enabled = currentRoute != Screen.Home.route) {
        when (currentRoute) {
            Screen.Add.route -> {
                addViewModel.clearState()
                currentRoute = Screen.Home.route
            }
            Screen.Edit.route -> {
                editingItem = null
                addViewModel.clearState()
                currentRoute = Screen.Home.route
            }
            Screen.Detail.route -> {
                viewingItem = null
                currentRoute = Screen.Home.route
            }
            Screen.Settings.route -> {
                settingsViewModel.refresh()
                currentRoute = Screen.Home.route
            }
            Screen.Trash.route -> {
                currentRoute = Screen.Settings.route
            }
            Screen.About.route -> {
                currentRoute = Screen.Settings.route
            }
            Screen.Donate.route -> {
                currentRoute = Screen.Settings.route
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 内容区：占满底部导航以外的全部空间
        Box(modifier = Modifier.weight(1f)) {
            when (currentRoute) {
                Screen.Home.route -> HomeRoute(
                    onAddClick = {
                        addViewModel.clearState()
                        currentRoute = Screen.Add.route
                    },
                    onGoToSettings = goToSettings,
                    onEditClick = { item ->
                        editingItem = item
                        addViewModel.clearState()
                        addViewModel.loadItem(item)
                        currentRoute = Screen.Edit.route
                    },
                    onItemClick = { item ->
                        viewingItem = item
                        currentRoute = Screen.Detail.route
                    },
                )

                Screen.Add.route -> AddItemRoute(
                    viewModel = addViewModel,
                    item = null,
                    onBack = { addViewModel.clearState(); currentRoute = Screen.Home.route },
                    onGoToSettings = goToSettings,
                )

                Screen.Edit.route -> AddItemRoute(
                    viewModel = addViewModel,
                    item = editingItem,
                    onBack = {
                        editingItem = null
                        addViewModel.clearState()
                        currentRoute = Screen.Home.route
                    },
                    onGoToSettings = goToSettings,
                )

                Screen.Detail.route -> ItemDetailScreen(
                    itemId = viewingItem?.id ?: 0L,
                    onBack = {
                        viewingItem = null
                        currentRoute = Screen.Home.route
                    },
                    onEdit = { item ->
                        editingItem = item
                        addViewModel.clearState()
                        addViewModel.loadItem(item)
                        currentRoute = Screen.Edit.route
                    },
                )

                Screen.Settings.route -> SettingsScreen(
                    onOpenTrash = { currentRoute = Screen.Trash.route },
                    onOpenAbout = { currentRoute = Screen.About.route },
                    onOpenDonate = { currentRoute = Screen.Donate.route },
                    listState = settingsListState,
                    onBack = {
                        settingsViewModel.refresh()
                        currentRoute = Screen.Home.route
                    },
                )

                Screen.Trash.route -> TrashScreen(
                    onBack = { currentRoute = Screen.Settings.route },
                )

                Screen.About.route -> AboutRoute(
                    onBack = { currentRoute = Screen.Settings.route },
                    onOpenDonate = { currentRoute = Screen.Donate.route },
                )

                Screen.Donate.route -> DonateRoute(
                    onBack = { currentRoute = Screen.Settings.route },
                )
            }
        }

        // 自定义底部导航栏（固定高度，避免 NavigationBar 高度异常问题）
        CustomBottomBar(
            currentRoute = currentRoute,
            onNavigate = { route ->
                // 离开“添加/编辑”页：从编辑页切到“添加”页同样会丢失当前编辑内容，也需要确认
                val leavingAddEdit = (currentRoute == Screen.Add.route || currentRoute == Screen.Edit.route) &&
                    route != currentRoute
                val leavingSettings = currentRoute == Screen.Settings.route &&
                    route != Screen.Settings.route
                when {
                    // 有未保存修改：拦截切换，先弹确认框
                    (leavingAddEdit && addHasChanges) || (leavingSettings && settingsHasChanges) -> {
                        pendingRoute = route
                        showDiscardDialog = true
                    }

                    // 点击当前所在 Tab：不做任何事（避免误触清空正在编辑的内容）
                    route == currentRoute -> Unit

                    else -> {
                        // 进入“添加”页前清空工作副本
                        if (route == Screen.Add.route) {
                            addViewModel.clearState()
                        }
                        // 离开“添加/编辑”页时丢弃未提交修改
                        if (leavingAddEdit) {
                            addViewModel.clearState()
                            if (currentRoute == Screen.Edit.route) {
                                editingItem = null
                            }
                        }
                        // 进入设置页时重新加载配置（丢弃上次未保存修改）
                        if (route == Screen.Settings.route) {
                            settingsViewModel.refresh()
                        }
                        currentRoute = route
                    }
                }
            },
            onTabClick = { if (navEmojis.size < 3) navEmojis.add(System.nanoTime()) },
        )
        }

        // 底部导航点击的 Emoji 彩蛋层（叠加在底部导航上方）
        navEmojis.forEach { id ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                EmojiEffect(onFinished = { navEmojis.remove(id) })
            }
        }
    }
}

/**
 * 自定义底部导航栏：固定 64dp 高度，三个标签（首页 / 添加 / 设置）。
 */
@Composable
private fun CustomBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onTabClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTabItem(
            label = "首页",
            icon = Icons.AutoMirrored.Filled.List,
            selected = currentRoute == Screen.Home.route,
            onClick = {
                onTabClick()
                onNavigate(Screen.Home.route)
            },
        )
        BottomTabItem(
            label = "添加",
            icon = Icons.Filled.Add,
            selected = currentRoute == Screen.Add.route,
            onClick = {
                onTabClick()
                onNavigate(Screen.Add.route)
            },
        )
        BottomTabItem(
            label = "设置",
            icon = Icons.Filled.Settings,
            selected = currentRoute == Screen.Settings.route ||
                currentRoute == Screen.About.route ||
                currentRoute == Screen.Donate.route,
            onClick = {
                onTabClick()
                onNavigate(Screen.Settings.route)
            },
        )
    }
}

/**
 * 底部导航单个标签项。
 */
@Composable
private fun RowScope.BottomTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}