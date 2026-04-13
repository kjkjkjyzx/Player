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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.player.model.VideoItem
import com.example.player.ui.theme.ChipSelected
import com.example.player.ui.theme.ChipUnselected
import com.example.player.ui.theme.DarkBackground
import com.example.player.ui.theme.DarkBorder
import com.example.player.ui.theme.DarkBorderLight
import com.example.player.ui.theme.DarkCard
import com.example.player.ui.theme.NavBarBg
import com.example.player.ui.theme.PrimaryBlue
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.ui.theme.TextTertiary
import com.example.player.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onVideoSelected: (Uri) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val videos by viewModel.videos.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()
    var selectedTab by remember { mutableStateOf("全部") }

    val filteredVideos = when (selectedTab) {
        "最近播放" -> videos.filter { (savedPositions[it.uri.toString()] ?: 0L) > 0L }
        else -> videos
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
        if (granted) {
            viewModel.loadVideos()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("需要存储权限才能浏览本地视频。请通过文件选择器打开视频。")
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(onAddClick = { filePicker.launch(arrayOf("video/*")) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 顶部标题区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "我的媒体库",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "视频",
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
                        .clickable { /* 筛选功能待实现 */ }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(text = "筛选", color = TextPrimary, fontSize = 13.sp)
                }
            }

            // 分类 Chip 横滚行
            val tabs = listOf("全部", "最近播放", "收藏", "文件夹")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) ChipSelected else ChipUnselected)
                            .border(0.5.dp, if (selected) Color.Transparent else DarkBorderLight, CircleShape)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) Color.Black else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            // 视频列表 / 空状态
            if (filteredVideos.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    selectedTab = selectedTab,
                    onPickFile = { filePicker.launch(arrayOf("video/*")) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVideos) { video ->
                        VideoCard(
                            video = video,
                            savedPositionMs = savedPositions[video.uri.toString()] ?: 0L,
                            onClick = { onVideoSelected(video.uri) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
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
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryBlue.copy(alpha = 0.25f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (selectedTab == "最近播放") "暂无播放记录" else "暂无本地视频",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (selectedTab == "最近播放") "开始播放一个视频后会显示在这里"
                   else "点击下方「＋」按钮选择视频文件",
            color = TextSecondary,
            fontSize = 13.sp
        )
        if (selectedTab == "全部") {
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PrimaryBlue)
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
private fun VideoCard(
    video: VideoItem,
    savedPositionMs: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
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
        // 缩略图区域（96dp 宽）
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
                val state = painter.state
                if (state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1A2A3C), Color(0xFF0D1826))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
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
                Text(
                    text = video.durationFormatted,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // 信息区
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = video.displayName.substringBeforeLast('.'),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = video.sizeFormatted,
                color = TextTertiary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            // 播放进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (savedPositionMs > 0L) "上次播放到 ${formatTime(savedPositionMs)}"
                       else "未播放",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

// ── 底部导航栏 ────────────────────────────────────────────────────────────────

@Composable
private fun BottomNavBar(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBarBg)
            .border(
                width = 0.5.dp,
                color = DarkBorder,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "媒体库", tint = TextPrimary, modifier = Modifier.size(22.dp)) }, label = "媒体库", active = true)
        NavItem(icon = { Icon(Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary, modifier = Modifier.size(22.dp)) }, label = "搜索", active = false)

        // 中间突起添加按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(text = "添加", color = TextSecondary, fontSize = 10.sp)
        }

        NavItem(icon = { Icon(Icons.Default.PlayArrow, contentDescription = "播放列表", tint = TextSecondary, modifier = Modifier.size(22.dp)) }, label = "列表", active = false)
        NavItem(icon = { Icon(Icons.Default.Person, contentDescription = "我的", tint = TextSecondary, modifier = Modifier.size(22.dp)) }, label = "我的", active = false)
    }
}

@Composable
private fun NavItem(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        icon()
        Text(
            text = label,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 10.sp
        )
    }
}

// ── 工具函数 ──────────────────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
