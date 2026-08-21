package com.example.smartstorage.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.smartstorage.data.local.prefs.STAR_REPO_URL
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import com.example.smartstorage.presentation.common.openUrlWithChooser
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.smartstorage.presentation.common.EmojiEffect
import com.example.smartstorage.presentation.detail.ItemDetailScreen
import com.example.smartstorage.presentation.donate.DonateRoute
import com.example.smartstorage.presentation.home.HomeRoute
import com.example.smartstorage.presentation.navigation.Screen
import com.example.smartstorage.presentation.onboarding.OnboardingScreen
import com.example.smartstorage.presentation.onboarding.OnboardingViewModel
import com.example.smartstorage.presentation.settings.SettingsScreen
import com.example.smartstorage.presentation.settings.SettingsViewModel
import com.example.smartstorage.presentation.trash.TrashScreen
import kotlinx.coroutines.launch

/**
 * 应用主界面：底部导航（首页 / 添加 / 设置）+ 页面容器。
 *
 * 三个主 Tab 使用 HorizontalPager 支持左右滑动切换（类似微信），底部导航与 Pager 双向绑定：
 * - 滑动 Pager 时同步更新底部导航选中态
 * - 点击底部导航时平滑滚动到对应页
 *
 * 编辑 / 详情 / 回收站 / 关于 / 捐赠等子页面以全屏覆盖层显示在 Pager 之上（不参与滑动）。
 * 有未保存修改时禁用 Pager 滑动，底部 Tab 点击仍走「放弃修改」确认框。
 */
@Composable
fun MainScreen() {
    // Pager 状态：三个主 Tab（0=首页、1=添加、2=设置）
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // 当前逻辑路由（含子页面）；主 Tab 状态下与 Pager 当前页保持一致
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }

    // 当前正在编辑的物品（null 表示新增）
    var editingItem by remember { mutableStateOf<Item?>(null) }

    // 当前查看详情的物品
    var viewingItem by remember { mutableStateOf<Item?>(null) }

    // 添加/编辑页共用 ViewModel；设置页 ViewModel
    val addViewModel: AddItemViewModel = hiltViewModel()

    // 全局上下文与 Star 里程碑提醒状态（弹窗提升到 MainScreen，保证添加成功后无论在哪页都能显示）
    val context = LocalContext.current
    val starReminder by addViewModel.starReminder.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    // 设置页滚动状态（提升到 MainScreen，进入子页面返回后仍保持滚动位置）
    val settingsListState = rememberLazyListState()

    // 是否有未保存修改（用于拦截滑动 / 底部 Tab 切换）
    val addHasChanges by addViewModel.hasChanges.collectAsStateWithLifecycle()
    val settingsHasChanges by settingsViewModel.hasChanges.collectAsStateWithLifecycle()

    // 是否显示首次启动引导页
    val showOnboarding by onboardingViewModel.showOnboarding.collectAsStateWithLifecycle()

    // 待确认的 Tab 切换目标（null 表示无）
    var pendingTab by remember { mutableStateOf<Int?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // 底部导航点击触发的 Emoji 彩蛋（最多 3 个并发）
    val navEmojis = remember { mutableStateListOf<Long>() }

    // 三个主 Tab 路由集合（判断当前是否处于主 Tab 而非子页面）
    val mainTabRoutes = setOf(Screen.Home.route, Screen.Add.route, Screen.Settings.route)

    /** 路由 → 主 Tab 索引（子页面归属到其来源 Tab） */
    fun routeToTab(route: String): Int = when (route) {
        Screen.Add.route, Screen.Edit.route -> 1
        Screen.Settings.route, Screen.Trash.route, Screen.About.route, Screen.Donate.route -> 2
        else -> 0
    }

    /** 主 Tab 索引 → 路由 */
    fun tabToRoute(tab: Int): String = when (tab) {
        1 -> Screen.Add.route
        2 -> Screen.Settings.route
        else -> Screen.Home.route
    }

    // Pager 滑动到位后：无子页面覆盖时同步 currentRoute（滑动 → 更新底部导航选中态）
    LaunchedEffect(pagerState.settledPage) {
        if (currentRoute in mainTabRoutes && pagerState.settledPage != routeToTab(currentRoute)) {
            currentRoute = tabToRoute(pagerState.settledPage)
        }
    }

    // 统一的「放弃修改」确认弹窗（底部 Tab 切换 / 滑动拦截用）
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingTab = null
            },
            title = { Text("放弃修改？") },
            text = { Text("确定要放弃已修改的内容吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        val target = pendingTab
                        pendingTab = null
                        // 丢弃未保存修改（工作副本直接作废，不写数据库、不删文件）
                        if (currentRoute == Screen.Add.route || currentRoute == Screen.Edit.route) {
                            addViewModel.clearState()
                        }
                        // 离开编辑页时清空正在编辑的物品，避免残留状态导致再次确认
                        if (currentRoute == Screen.Edit.route) {
                            editingItem = null
                        }
                        if (target != null) {
                            val targetRoute = tabToRoute(target)
                            if (targetRoute == Screen.Settings.route) {
                                settingsViewModel.refresh()
                            }
                            currentRoute = targetRoute
                            scope.launch { pagerState.animateScrollToPage(target) }
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
                        pendingTab = null
                    },
                ) {
                    Text("继续编辑")
                }
            },
        )
    }

    // 统一的 Tab 切换逻辑（底部导航点击）：有未保存修改先弹确认框，否则滚动 Pager
    fun navigateToTab(tab: Int) {
        val targetRoute = tabToRoute(tab)
        // 离开“添加/编辑”页：从编辑页切到“添加”页同样会丢失当前编辑内容，也需要确认
        val leavingAddEdit = (currentRoute == Screen.Add.route || currentRoute == Screen.Edit.route) &&
            targetRoute != currentRoute
        val leavingSettings = currentRoute == Screen.Settings.route &&
            targetRoute != Screen.Settings.route
        when {
            // 有未保存修改：拦截切换，先弹确认框
            (leavingAddEdit && addHasChanges) || (leavingSettings && settingsHasChanges) -> {
                pendingTab = tab
                showDiscardDialog = true
            }

            // 点击当前所在 Tab：不做任何事（避免误触清空正在编辑的内容）
            targetRoute == currentRoute -> Unit

            else -> {
                // 进入“添加”页前清空工作副本
                if (targetRoute == Screen.Add.route) {
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
                if (targetRoute == Screen.Settings.route) {
                    settingsViewModel.refresh()
                }
                currentRoute = targetRoute
                // 平滑滚动 Pager 到目标 Tab
                scope.launch { pagerState.animateScrollToPage(tab) }
            }
        }
    }

    // “去配置”：跳转设置页 AI 配置（丢弃添加/编辑页未保存内容）
    val goToSettings: () -> Unit = {
        addViewModel.clearState()
        settingsViewModel.refresh()
        currentRoute = Screen.Settings.route
        scope.launch { pagerState.animateScrollToPage(2) }
    }

    // 全面屏返回/系统返回：非首页返回上一级（首页保持默认退出；页面自身的 BackHandler 优先）
    BackHandler(enabled = currentRoute != Screen.Home.route) {
        when (currentRoute) {
            Screen.Add.route -> {
                addViewModel.clearState()
                currentRoute = Screen.Home.route
                scope.launch { pagerState.animateScrollToPage(0) }
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
                scope.launch { pagerState.animateScrollToPage(0) }
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

    when (showOnboarding) {
        // 首次启动：全屏展示引导页（不显示底部导航）
        true -> OnboardingScreen(onFinish = onboardingViewModel::onFinished)

        // 非首次启动：直接进入主界面
        false -> {
            // 有未保存修改时禁用 Pager 滑动（仅禁滑，底部 Tab 点击仍弹确认框）
            val pagerScrollEnabled = !(
                (currentRoute == Screen.Add.route && addHasChanges) ||
                    (currentRoute == Screen.Settings.route && settingsHasChanges)
                )
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 内容区：占满底部导航以外的全部空间
                    Box(modifier = Modifier.weight(1f)) {
                        // 主 Tab：HorizontalPager 左右滑动切换
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = pagerScrollEnabled,
                        ) { page ->
                            when (page) {
                                0 -> HomeRoute(
                                    onAddClick = {
                                        addViewModel.clearState()
                                        currentRoute = Screen.Add.route
                                        scope.launch { pagerState.animateScrollToPage(1) }
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

                                1 -> AddItemRoute(
                                    viewModel = addViewModel,
                                    item = null,
                                    onBack = {
                                        addViewModel.clearState()
                                        currentRoute = Screen.Home.route
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    onGoToSettings = goToSettings,
                                )

                                2 -> SettingsScreen(
                                    onOpenTrash = { currentRoute = Screen.Trash.route },
                                    onOpenAbout = { currentRoute = Screen.About.route },
                                    onOpenDonate = { currentRoute = Screen.Donate.route },
                                    listState = settingsListState,
                                    onBack = {
                                        settingsViewModel.refresh()
                                        currentRoute = Screen.Home.route
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                )
                            }
                        }

                        // 子页面覆盖层：带不透明背景，避免与底部 Pager 内容重叠（编辑 / 详情 / 回收站 / 关于 / 捐赠）
                        if (currentRoute !in mainTabRoutes) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                            ) {
                                when (currentRoute) {
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
                        }
                    }

                    // 自定义底部导航栏（固定高度，避免 NavigationBar 高度异常问题）
                    CustomBottomBar(
                        currentPage = pagerState.currentPage,
                        onNavigate = { tab -> navigateToTab(tab) },
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

        // GitHub Star 里程碑提醒（全局弹窗：添加成功后弹出）
        starReminder?.let { milestone ->
            AlertDialog(
                onDismissRequest = { addViewModel.onStarRemindLater() },
                title = { Text("🎉 恭喜你已添加 $milestone 件物品！") },
                text = {
                    Text(
                        "如果你觉得这个 App 对你有帮助，欢迎到 GitHub 给项目点个 Star ⭐\n" +
                            "这对我非常重要，也是我继续更新的动力！",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 打开 GitHub 仓库并标记该里程碑已永久提醒
                            openUrlWithChooser(context, STAR_REPO_URL)
                            addViewModel.onStarGoToGithub(milestone)
                        },
                    ) {
                        Text("去 GitHub 点 Star")
                    }
                },
                dismissButton = {
                    TextButton(onClick = addViewModel::onStarRemindLater) {
                        Text("稍后提醒")
                    }
                },
            )
        }
        }

        // 读取中（null）：短暂空白，避免非首次用户闪一下引导页
        else -> Unit
    }
}

/**
 * 自定义底部导航栏：固定 64dp 高度，三个标签（首页 / 添加 / 设置）。
 * 选中态直接读取 Pager 当前页，与滑动切换保持同步。
 */
@Composable
private fun CustomBottomBar(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
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
            selected = currentPage == 0,
            onClick = {
                onTabClick()
                onNavigate(0)
            },
        )
        BottomTabItem(
            label = "添加",
            icon = Icons.Filled.Add,
            selected = currentPage == 1,
            onClick = {
                onTabClick()
                onNavigate(1)
            },
        )
        BottomTabItem(
            label = "设置",
            icon = Icons.Filled.Settings,
            selected = currentPage == 2,
            onClick = {
                onTabClick()
                onNavigate(2)
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
