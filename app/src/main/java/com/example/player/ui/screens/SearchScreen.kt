package com.example.player.ui.screens

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.player.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.ui.components.LiquidGlassContainer
import com.example.player.ui.components.StarryBackground
import com.example.player.util.PinyinSearch
import com.example.player.ui.theme.GradientStart
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.viewmodel.HomeViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.player.ui.theme.AppSpring
import com.example.player.ui.theme.GlassDefaults

/**
 * 搜索页筛选维度。与文本 query 做 AND 组合。
 */
private enum class SearchFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    FAVORITE(R.string.filter_favorite),
    LANDSCAPE(R.string.filter_landscape),
    RECENT(R.string.filter_recent),
    LARGE(R.string.filter_large);
}

private const val ONE_GB_BYTES: Long = 1_073_741_824L

@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    onVideoSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val videos         by viewModel.videos.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()
    val favorites      by viewModel.favorites.collectAsState()
    var query          by remember { mutableStateOf("") }
    var filter         by remember { mutableStateOf(SearchFilter.ALL) }

    // 命中规则：原文 contains → 全拼 contains → 拼音首字母 contains（见 PinyinSearch）
    // 筛选维度与文本 query 做 AND 组合；空 query 时即便有筛选也返回空列表（避免误导）。
    // 使用 remember(query, filter, videos, favorites, savedPositions) 避免滚动时重复计算
    val results = remember(query, filter, videos, favorites, savedPositions) {
        if (query.isBlank()) emptyList()
        else videos.asSequence()
            .filter { PinyinSearch.matches(it.displayName, query) }
            .filter { v ->
                when (filter) {
                    SearchFilter.ALL       -> true
                    SearchFilter.FAVORITE  -> favorites.contains(v.uri.toString())
                    SearchFilter.LANDSCAPE -> v.isLandscape
                    SearchFilter.RECENT    -> (savedPositions[v.uri.toString()] ?: 0L) > 0L
                    SearchFilter.LARGE     -> v.size >= ONE_GB_BYTES
                }
            }
            .toList()
    }

    StarryBackground(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ── 标题行 ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = stringResource(R.string.search_title),
                color      = TextPrimary,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        // ── 搜索栏（返回按钮 + 胶囊输入框）──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 圆形玻璃返回按钮
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawRect(brush = GlassDefaults.backgroundBrush)
                        drawRect(
                            brush = GlassDefaults.highlightBrush,
                            size  = Size(size.width, GlassDefaults.highlightHeight.toPx())
                        )
                        drawRect(
                            brush   = GlassDefaults.bottomHighlightBrush,
                            topLeft = Offset(0f, size.height - GlassDefaults.bottomHighlightHeight.toPx()),
                            size    = Size(size.width, GlassDefaults.bottomHighlightHeight.toPx())
                        )
                    }
                    .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 胶囊形搜索框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .drawBehind {
                        drawRect(brush = GlassDefaults.backgroundBrush)
                        drawRect(
                            brush = GlassDefaults.highlightBrush,
                            size  = Size(size.width, GlassDefaults.highlightHeight.toPx())
                        )
                        drawRect(
                            brush   = GlassDefaults.bottomHighlightBrush,
                            topLeft = Offset(0f, size.height - GlassDefaults.bottomHighlightHeight.toPx()),
                            size    = Size(size.width, GlassDefaults.bottomHighlightHeight.toPx())
                        )
                    }
                    .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, RoundedCornerShape(50.dp))
            ) {
                TextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text(stringResource(R.string.search_placeholder), color = TextSecondary, fontSize = 15.sp) },
                    leadingIcon   = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = TextPrimary
                    ),
                    shape    = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── 筛选 Chip 行（始终显示，让用户明确筛选范围） ───────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilter.entries.forEach { f ->
                val selected    = f == filter
                val label       = stringResource(f.labelRes)
                // iOS 26 弹性选中：背景透明度与文字颜色通过弹簧物理平滑过渡
                val bgAlpha     by animateFloatAsState(if (selected) 0.92f else 0.06f, AppSpring.standard(), label = "chipBg")
                val borderAlpha by animateFloatAsState(if (selected) 0.0f  else 0.18f, AppSpring.standard(), label = "chipBorder")
                val textColor   by animateColorAsState(if (selected) GradientStart else TextPrimary, AppSpring.standard(), label = "chipText")
                val textWeight  = if (selected) FontWeight.SemiBold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = bgAlpha))
                        .border(0.5.dp, Color.White.copy(alpha = borderAlpha), CircleShape)
                        .clickable { filter = f }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(label, color = textColor, fontSize = 12.sp, fontWeight = textWeight)
                }
            }
        }

        // iOS 26：内容区状态切换用 AnimatedContent，在"空查询/无结果/有结果"之间弹性淡入淡出
        val contentKey = when {
            query.isBlank()   -> 0   // 空查询提示
            results.isEmpty() -> 1   // 无匹配结果
            else              -> 2   // 结果列表
        }
        AnimatedContent(
            targetState  = contentKey,
            transitionSpec = {
                fadeIn(AppSpring.standard()) togetherWith fadeOut(AppSpring.gentle())
            },
            label = "searchContent"
        ) { key ->
            when (key) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.search_empty_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.search_empty_desc), color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.search_not_found), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text("\"$query\"", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    Column {
                        Text(
                            text       = stringResource(R.string.search_result_count, results.size),
                            color      = TextSecondary,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(results, key = { it.uri.toString() }, contentType = { "video" }) { video ->
                                VideoCard(
                                    video           = video,
                                    savedPositionMs = savedPositions[video.uri.toString()] ?: 0L,
                                    isFavorite      = favorites.contains(video.uri.toString()),
                                    onFavoriteClick = { viewModel.toggleFavorite(video.uri) },
                                    onClick         = { onVideoSelected(video.uri) },
                                    modifier        = Modifier.animateItem(
                                        placementSpec = AppSpring.standard(),
                                        fadeInSpec    = AppSpring.gentle(),
                                        fadeOutSpec   = AppSpring.gentle()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    } // StarryBackground
}
