package com.example.smartstorage.presentation.common

import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import java.io.File

/**
 * 全屏照片预览对话框：添加/编辑页与详情页共用的查看器。
 *
 * 支持：完整显示并保持原始比例；双指缩放（约 1x–5x）；放大后单指拖动；
 * 双击在 1x 与 2.5x 之间切换；左右滑动切换多张照片并显示“当前 / 总数”；
 * 缩放 >1 时禁止左右切图以避免手势冲突；切换照片时重置缩放与位移。
 * 关闭入口：右上角关闭按钮 + 系统返回；关闭后不影响底层页面内容。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPreviewDialog(
    imagePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (imagePaths.isEmpty()) return
    val startIndex = initialIndex.coerceIn(0, imagePaths.lastIndex)
    val pagerState = rememberPagerState(initialPage = startIndex) { imagePaths.size }

    // 当前页的缩放与位移（随页面切换重置，避免沿用上一张的状态）
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 切页时重置缩放与位置
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    // 外部列表变化导致当前页越界时自动关闭（防御性处理）
    LaunchedEffect(imagePaths.size) {
        if (imagePaths.isEmpty() || pagerState.currentPage >= imagePaths.size) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // 放大后禁止左右切图，优先让手势用于拖动查看细节
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale <= 1f,
                key = { imagePaths[it] },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomablePhotoPage(
                    path = imagePaths[page],
                    scale = if (page == pagerState.currentPage) scale else 1f,
                    offset = if (page == pagerState.currentPage) offset else Offset.Zero,
                    onScaleChange = { scale = it },
                    onOffsetChange = { offset = it },
                )
            }

            // 页码提示（仅多图时显示）
            if (imagePaths.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )
            }

            // 关闭入口（系统返回同样会触发 onDismissRequest）
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.photo_preview_close),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/**
 * 单张可缩放照片：手势与 HorizontalPager 的切图手势做互斥——
 * 未放大时单指滑动交给 Pager 切图；放大后（或双指操作时）由本组件消费用于缩放/拖动。
 */
@Composable
private fun ZoomablePhotoPage(
    path: String,
    scale: Float,
    offset: Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
) {
    // 用 rememberUpdatedState 让 pointerInput 内读取到最新值（避免闭包捕获旧状态）
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = File(path),
            contentDescription = stringResource(R.string.photo_preview_cd),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    // 双击：1x 与 2.5x 之间切换
                    detectTapGestures(
                        onDoubleTap = {
                            if (currentScale > 1f) {
                                onScaleChange(1f)
                                onOffsetChange(Offset.Zero)
                            } else {
                                onScaleChange(2.5f)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    // 缩放 + 拖动：双指捏合缩放；放大后单指拖动查看细节
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val pressedCount = event.changes.count { it.pressed }
                            val isPinch = pressedCount >= 2 || zoomChange != 1f
                            val oldScale = currentScale
                            if (isPinch || oldScale > 1f) {
                                val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                                onScaleChange(newScale)
                                if (newScale > 1f) {
                                    // 拖动位移按容器边界钳制，避免内容被拖出屏幕后找不回来
                                    val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                    val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                    val base = if (oldScale > 1f) currentOffset else Offset.Zero
                                    onOffsetChange(
                                        Offset(
                                            (base.x + panChange.x).coerceIn(-maxX, maxX),
                                            (base.y + panChange.y).coerceIn(-maxY, maxY),
                                        ),
                                    )
                                } else {
                                    onOffsetChange(Offset.Zero)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.photo_preview_error),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        )
    }
}