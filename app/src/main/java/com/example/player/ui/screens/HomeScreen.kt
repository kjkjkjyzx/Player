package com.example.player.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.ui.theme.DarkBackground
import com.example.player.ui.theme.DarkBorder
import com.example.player.ui.theme.DarkCard
import com.example.player.ui.theme.DarkSurface
import com.example.player.ui.theme.NavBarBg
import com.example.player.ui.theme.PrimaryBlue
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.ui.theme.TextTertiary
import com.example.player.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onVideoSelected: (Uri) -> Unit
) {
    val scope            = rememberCoroutineScope()
    val snackbarState    = remember { SnackbarHostState() }
    val lifecycleOwner   = LocalLifecycleOwner.current

    // 从播放器返回时刷新播放进度
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPositions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val videos           by viewModel.videos.collectAsState()
    val savedPositions   by viewModel.savedPositions.collectAsState()
    val favorites        by viewModel.favorites.collectAsState()

    var selectedTab      by remember { mutableStateOf("全部") }
    var showFilterSheet  by remember { mutableStateOf(false) }
    val sheetState       = rememberModalBottomSheetState()

    val filteredVideos = when (selectedTab) {
        "最近播放" -> videos.filter { (savedPositions[it.uri.toString()] ?: 0L) > 0L }
        "收藏"     -> videos.filter { favorites.contains(it.uri.toString()) }
        "文件夹"   -> videos.sortedBy { it.displayName.substringBeforeLast('/') }
        else       -> videos
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onVideoSelected(it) } }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_VIDEO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadVideos()
        else scope.launch { snackbarState.showSnackbar("需要存储权限才能浏览本地视频") }
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(permission) }

    Scaffold(
        snackbarHost  = { SnackbarHost(snackbarState) },
        containerColor = DarkBackground,
        bottomBar     = {
            BottomNavBar(
                navController = navController,
                onAddClick    = { filePicker.launch(arrayOf("video/*")) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 顶部标题 ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("我的媒体库", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.3.sp)
                    Text(
                        "视频",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x14FFFFFF))
                        .border(0.5.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                        .clickable { showFilterSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (viewModel.currentSort == SortOption.DATE_DESC) "筛选"
                               else viewModel.currentSort.label,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }

            // ── 分类 Chip 行 ───────────────────────────────────────────────
            val tabs = listOf("全部", "最近播放", "收藏", "文件夹")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    // 液态玻璃 Chip：选中为深色磨砂玻璃，未选中为透明磨砂玻璃
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selected)
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3D3830), Color(0xFF252017)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.50f)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (selected) listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.04f),
                                        Color.White.copy(alpha = 0.20f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.55f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = CircleShape
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            // ── 视频列表 / 空状态 ──────────────────────────────────────────
            if (filteredVideos.isEmpty()) {
                EmptyState(
                    modifier    = Modifier.weight(1f).fillMaxWidth(),
                    selectedTab = selectedTab,
                    onPickFile  = { filePicker.launch(arrayOf("video/*")) }
                )
            } else {
                LazyColumn(
                    modifier        = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVideos, key = { it.uri.toString() }) { video ->
                        VideoCard(
                            video          = video,
                            savedPositionMs = savedPositions[video.uri.toString()] ?: 0L,
                            isFavorite     = favorites.contains(video.uri.toString()),
                            onFavoriteClick = { viewModel.toggleFavorite(video.uri) },
                            onClick        = { onVideoSelected(video.uri) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    // ── 筛选弹窗 ──────────────────────────────────────────────────────────
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState       = sheetState,
            containerColor   = DarkSurface,
            tonalElevation   = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("排序方式", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(12.dp))
                SortOption.entries.forEach { option ->
                    val selected = option == viewModel.currentSort
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0x1A000000) else Color.Transparent)
                            .clickable {
                                viewModel.setSortOption(option)
                                showFilterSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(option.label, color = TextPrimary, fontSize = 15.sp)
                        if (selected) {
                            androidx.compose.material3.Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint     = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

// ── 空状态 ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    selectedTab: String,
    onPickFile: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(PrimaryBlue.copy(alpha = 0.25f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint     = PrimaryBlue,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = when (selectedTab) {
                "最近播放" -> "暂无播放记录"
                "收藏"     -> "还没有收藏的视频"
                "文件夹"   -> "暂无视频"
                else       -> "暂无本地视频"
            },
            color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (selectedTab) {
                "最近播放" -> "开始播放一个视频后会显示在这里"
                "收藏"     -> "长按视频卡片上的♡图标即可收藏"
                else       -> "点击下方「＋」按钮选择视频文件"
            },
            color = TextSecondary, fontSize = 14.sp
        )
        if (selectedTab == "全部") {
            Spacer(Modifier.height(28.dp))
            // 液态玻璃蓝色按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2980E8), Color(0xFF1255A8)),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.35f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onPickFile)
                    .padding(horizontal = 28.dp, vertical = 13.dp)
            ) {
                Text("选择视频", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 视频卡片 ──────────────────────────────────────────────────────────────────

@Composable
fun VideoCard(
    video: VideoItem,
    savedPositionMs: Long,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context  = LocalContext.current
    val progress = if (video.duration > 0L) savedPositionMs.toFloat() / video.duration else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略图
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.uri)
                    .setParameter("coil#video_frame_micros", 1_000_000L)
                    .build(),
                contentDescription = null
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1A2A3C), Color(0xFF0D1826)))),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint     = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            // 时长标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(video.durationFormatted, color = Color.White, fontSize = 10.sp)
            }
        }

        // 信息区
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
        ) {
            Text(
                text       = video.displayName.substringBeforeLast('.'),
                color      = TextPrimary,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(video.sizeFormatted, color = TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            // 播放进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.Black.copy(alpha = 0.10f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(TextPrimary)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text     = if (savedPositionMs > 0L) "上次播放到 ${formatTime(savedPositionMs)}" else "未播放",
                color    = TextTertiary,
                fontSize = 11.sp
            )
        }

        // 收藏按钮
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onFavoriteClick),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint     = if (isFavorite) Color(0xFFE53935) else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── 底部导航栏 ────────────────────────────────────────────────────────────────

private data class NavDest(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BottomNavBar(
    navController: NavController,
    onAddClick: () -> Unit
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute  = currentEntry?.destination?.route

    val items = listOf(
        NavDest("home",    Icons.Default.VideoLibrary, "媒体库"),
        NavDest("search",  Icons.Default.Search,       "搜索"),
        NavDest("list",    Icons.Default.Queue,        "列表"),
        NavDest("profile", Icons.Default.Person,       "我的")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBarBg)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, dest ->
            // 中间插入突起添加按钮
            if (index == 2) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 液态玻璃添加按钮
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.55f)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 1.00f),
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.60f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = CircleShape
                            )
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Add,
                            contentDescription = "添加",
                            tint     = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text("添加", color = TextSecondary, fontSize = 10.sp)
                }
            }

            val active = currentRoute == dest.route
            Column(
                modifier            = Modifier
                    .clickable {
                        if (currentRoute != dest.route) {
                            navController.navigate(dest.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.material3.Icon(
                    dest.icon,
                    contentDescription = dest.label,
                    tint     = if (active) TextPrimary else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(dest.label, color = if (active) TextPrimary else TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

// ── 工具函数 ──────────────────────────────────────────────────────────────────

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
