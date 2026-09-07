package com.example.smartstorage.presentation.onboarding

import androidx.compose.ui.res.stringResource
import com.example.smartstorage.R

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** 引导页单页数据：标题、描述与插图图标。 */
private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
)

/** 引导页三页内容（文案从资源读取，随语言切换）。 */
@Composable
private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        title = stringResource(R.string.onboarding_1_title),
        description = stringResource(R.string.onboarding_1_sub),
        icon = Icons.Outlined.Inventory,
    ),
    OnboardingPage(
        title = stringResource(R.string.onboarding_2_title),
        description = stringResource(R.string.onboarding_2_sub),
        icon = Icons.Outlined.AutoAwesome,
    ),
    OnboardingPage(
        title = stringResource(R.string.onboarding_3_title),
        description = stringResource(R.string.onboarding_3_sub),
        icon = Icons.Outlined.Search,
    ),
)

/**
 * 首次启动引导页：全屏展示 3 页，支持左右滑动、跳过、下一步 / 开始使用。
 *
 * @param onFinish 完成引导（点击“跳过”或最后一页“开始使用”）后的回调
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    // 页码状态：rememberPagerState 在屏幕旋转/进程重建时自动保持当前页
    val pages = onboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 背景：从 primaryContainer 到 surface 的垂直渐变
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
    ) {
        // 右上角“跳过”按钮：点击直接完成引导
        TextButton(
            onClick = onFinish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }

        // 中间：三页内容（图标 + 标题 + 描述）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 插图（大图标）
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(32.dp))
                // 标题
                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 描述文字
                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // 底部区域：页码指示器 + 下一步 / 开始使用按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 页码指示器：3 个圆点，当前页为药丸形高亮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 24.dp else 8.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 主按钮：非最后一页显示“下一步”，最后一页显示“开始使用”
            Button(
                onClick = {
                    val isLastPage = pagerState.currentPage == pages.lastIndex
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex) stringResource(R.string.onboarding_start) else stringResource(R.string.onboarding_next),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}