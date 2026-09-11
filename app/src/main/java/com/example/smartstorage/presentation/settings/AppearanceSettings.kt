package com.example.smartstorage.presentation.settings

import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartstorage.data.local.prefs.GradientDirection
import com.example.smartstorage.data.local.prefs.TextColorConfig
import com.example.smartstorage.data.local.prefs.TextColorType
import com.example.smartstorage.data.local.prefs.ThemeMode
import com.example.smartstorage.presentation.theme.textColorStyle
import kotlinx.coroutines.delay

/** 外观设置分组在设置页 LazyColumn 中的 item 下标（顺序：通用设置 0 / 外观设置 1 / AI 2 / 数据管理 3 / 关于 4） */
internal const val APPEARANCE_ITEM_INDEX = 1

/** 纯色预设：沿用原「输入文字颜色」的 5 种颜色（名称用于下方小字标注） */
private val solidPresets = listOf(
    Color(0xFF111111) to R.string.color_black,
    Color(0xFF0D47A1) to R.string.color_blue,
    Color(0xFF424242) to R.string.color_grey,
    Color(0xFF1B5E20) to R.string.color_green,
    Color(0xFF4E342E) to R.string.color_brown,
)

/** 渐变预设：红→橙、蓝→紫、绿→青、黑→灰 */
private val gradientPresets = listOf(
    Triple(Color(0xFFE53935), Color(0xFFFB8C00), R.string.gradient_red_orange),
    Triple(Color(0xFF1E88E5), Color(0xFF8E24AA), R.string.gradient_blue_purple),
    Triple(Color(0xFF43A047), Color(0xFF00ACC1), R.string.gradient_green_cyan),
    Triple(Color(0xFF212121), Color(0xFF757575), R.string.gradient_black_grey),
)

/** ARGB Int 转无符号 Long（供 TextColorConfig 存储） */
private fun argbLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL

/**
 * 外观设置分组：主题模式（跟随系统 / 浅色 / 深色）+ 文字颜色抽屉（完整选择器）。
 *
 * 文字颜色抽屉：折叠箭头旋转 + 展开/收起动画 + 展开时自动滚动到屏幕中间偏上。
 * 所有选择即时通过 [onTextColorConfigChange] 持久化，首页/详情/输入框实时生效。
 *
 * @param themeMode 当前主题模式
 * @param textColorConfig 当前全局文字颜色配置
 * @param onThemeModeSelect 切换主题模式回调（即时保存）
 * @param onTextColorConfigChange 更新文字颜色配置回调（即时保存）
 * @param listState 设置页 LazyColumn 状态（用于展开抽屉时自动滚动）
 */
@Composable
fun AppearanceSettingsGroup(
    themeMode: ThemeMode,
    textColorConfig: TextColorConfig,
    onThemeModeSelect: (ThemeMode) -> Unit,
    onTextColorConfigChange: (TextColorConfig) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    // 文字颜色抽屉展开状态（保存到 Bundle，旋转 / 进程重建后保持）
    var textColorExpanded by rememberSaveable { mutableStateOf(false) }

    // 展开抽屉时自动滚动到外观设置分组，确保内容完整展示（延迟 50ms 等待动画启动）
    LaunchedEffect(textColorExpanded) {
        if (textColorExpanded) {
            delay(50)
            // 正值 scrollOffset 使抽屉内容滚动到屏幕中间偏上；若实测方向相反仅取反该常量
            listState.animateScrollToItem(APPEARANCE_ITEM_INDEX, scrollOffset = 100)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 分组标题：小字、灰色、Medium，与卡片间距 4dp
        Text(
            text = stringResource(R.string.settings_appearance),
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
                // ===== 1. 主题模式：跟随系统 / 浅色 / 深色 =====
                SettingsListItem(
                    item = SettingsItem(
                        label = stringResource(R.string.settings_dark_mode),
                        icon = Icons.Outlined.DarkMode,
                        onClick = {},
                        // 主题模式为常驻展开，不显示右侧箭头
                        trailingIcon = {},
                        content = {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                            ) {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = { onThemeModeSelect(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ThemeMode.entries.size,
                                        ),
                                    ) {
                                        Text(stringResource(mode.labelRes))
                                    }
                                }
                            }
                        },
                    ),
                    showDivider = true,
                )

                // ===== 2. 文字颜色：抽屉（折叠箭头 + 展开动画 + 完整选择器）=====
                SettingsListItem(
                    item = SettingsItem(
                        label = stringResource(R.string.settings_text_color),
                        icon = Icons.Filled.Palette,
                        onClick = { textColorExpanded = !textColorExpanded },
                        // 抽屉箭头：收起显示 →，展开旋转 90° 变成 ↓
                        trailingIcon = { DrawerArrow(expanded = textColorExpanded) },
                        content = {
                            AnimatedVisibility(
                                visible = textColorExpanded,
                                enter = expandVertically(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(200)) +
                                    fadeOut(animationSpec = tween(200)),
                            ) {
                                TextColorDrawerContent(
                                    config = textColorConfig,
                                    onConfigChange = onTextColorConfigChange,
                                )
                            }
                        },
                    ),
                    showDivider = false,
                )
            }
        }
    }
}

/**
 * 抽屉箭头：收起状态显示水平箭头，展开状态旋转 90° 变成向下箭头。
 * 使用 animateFloatAsState 实现 150ms 平滑旋转动画。
 */
@Composable
private fun DrawerArrow(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "drawerArrowRotation",
    )
    Icon(
        imageVector = Icons.Outlined.KeyboardArrowRight,
        contentDescription = if (expanded) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.graphicsLayer { rotationZ = rotation },
    )
}

/**
 * 文字颜色抽屉内容：预览区 + 类型选择（默认/纯色/渐变）+ 预设颜色/渐变 + 自定义色环。
 */
@Composable
private fun TextColorDrawerContent(
    config: TextColorConfig,
    onConfigChange: (TextColorConfig) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 预览区：示例文字实时应用当前颜色配置（纯色/渐变即时可见）
        Text(
            text = stringResource(R.string.appearance_demo),
            style = textColorStyle(
                config = config,
                baseStyle = MaterialTheme.typography.bodyMedium,
                defaultColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        // 类型选择：默认 / 纯色 / 渐变
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TextColorType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = config.type == type,
                    onClick = { onConfigChange(config.copy(type = type)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TextColorType.entries.size,
                    ),
                ) {
                    Text(stringResource(type.labelRes))
                }
            }
        }

        when (config.type) {
            // 默认：跟随主题，无需额外配置
            TextColorType.DEFAULT -> Text(
                text = stringResource(R.string.settings_default_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 纯色：预设 + 自定义色环
            TextColorType.SOLID -> SolidColorContent(config, onConfigChange)

            // 渐变：预设 + 方向 + 自定义起始/结束色环
            TextColorType.GRADIENT -> GradientColorContent(config, onConfigChange)
        }
    }
}

/** 纯色配置区：预设 5 色 + 自定义色环 */
@Composable
private fun SolidColorContent(
    config: TextColorConfig,
    onConfigChange: (TextColorConfig) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_preset),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        solidPresets.forEach { (color, nameRes) ->
            val selected = config.solidColor == argbLong(color)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 3.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape,
                        )
                        .clickable {
                            onConfigChange(
                                config.copy(
                                    type = TextColorType.SOLID,
                                    solidColor = argbLong(color),
                                ),
                            )
                        },
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = stringResource(nameRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Text(
        text = stringResource(R.string.settings_custom_color),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // 自定义色环：选中的颜色实时写入纯色配置
    HsvColorPicker(
        color = Color(config.solidColor),
        onColorChange = { c ->
            onConfigChange(
                config.copy(
                    type = TextColorType.SOLID,
                    solidColor = argbLong(c),
                ),
            )
        },
    )
}

/** 渐变配置区：预设 4 组 + 方向选择 + 自定义起始/结束色环 */
@Composable
private fun GradientColorContent(
    config: TextColorConfig,
    onConfigChange: (TextColorConfig) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_preset_gradient),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        gradientPresets.forEach { (start, end, nameRes) ->
            val selected = config.gradientStart == argbLong(start) &&
                config.gradientEnd == argbLong(end)
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(start, end)))
                    .border(
                        width = 2.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        onConfigChange(
                            config.copy(
                                type = TextColorType.GRADIENT,
                                gradientStart = argbLong(start),
                                gradientEnd = argbLong(end),
                            ),
                        )
                    },
            )
        }
    }

    Text(
        text = stringResource(R.string.settings_gradient_dir),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        GradientDirection.entries.forEachIndexed { index, direction ->
            SegmentedButton(
                selected = config.direction == direction,
                onClick = { onConfigChange(config.copy(direction = direction)) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = GradientDirection.entries.size,
                ),
            ) {
                Text(stringResource(direction.labelRes))
            }
        }
    }

    // 自定义起始 / 结束色：两个标签切换共用同一个色环
    var targetEnd by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !targetEnd,
            onClick = { targetEnd = false },
            label = { Text(stringResource(R.string.settings_color_start)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        FilterChip(
            selected = targetEnd,
            onClick = { targetEnd = true },
            label = { Text(stringResource(R.string.settings_color_end)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
    HsvColorPicker(
        color = if (targetEnd) Color(config.gradientEnd) else Color(config.gradientStart),
        onColorChange = { c ->
            val argb = argbLong(c)
            onConfigChange(
                config.copy(
                    type = TextColorType.GRADIENT,
                    gradientStart = if (targetEnd) config.gradientStart else argb,
                    gradientEnd = if (targetEnd) argb else config.gradientEnd,
                ),
            )
        },
    )
}
