package com.example.player.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.player.model.Folder
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.ui.components.LiquidGlassContainer
import com.example.player.ui.components.StarryBackground
import com.example.player.ui.theme.DarkBorder
import com.example.player.ui.theme.DarkSurface
import com.example.player.ui.theme.GradientStart
import com.example.player.ui.theme.DarkCard
import com.example.player.ui.theme.NavBarBg
import com.example.player.ui.theme.PrimaryBlue
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.ui.theme.TextTertiary
import com.example.player.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(),
    onVideoSelected: (Uri) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPositions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val videos         by viewModel.videos.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()
    val favorites      by viewModel.favorites.collectAsState()
    val folders        by viewModel.folders.collectAsState()

    var selectedTab         by remember { mutableStateOf("全部") }
    var showFilterSheet     by remember { mutableStateOf(false) }
    var selectedFolder      by remember { mutableStateOf<Folder?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderDialogText by remember { mutableStateOf("") }
    var folderToDelete      by remember { mutableStateOf<Folder?>(null) }
    var videoToRemove       by remember { mutableStateOf<VideoItem?>(null) }
    var videoToHide         by remember { mutableStateOf<VideoItem?>(null) }

    // 拖拽状态
    var draggingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var dragOffset    by remember { mutableStateOf(Offset.Zero) }
    // folderId → 在根布局中的矩形（由 FolderDropTarget 更新）
    val folderBoundsMap = remember { mutableMapOf<String, Rect>() }

    val sheetState = rememberModalBottomSheetState()

    val filteredVideos = when (selectedTab) {
        "最近播放" -> videos.filter { (savedPositions[it.uri.toString()] ?: 0L) > 0L }
        "收藏"     -> videos.filter { favorites.contains(it.uri.toString()) }
        else       -> videos
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) viewModel.importVideos(uris) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadVideos()
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_VIDEO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    val glassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.80f),
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.40f)
        ),
        start = Offset.Zero,
        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    StarryBackground(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost   = { },
        containerColor = Color.Transparent,
        bottomBar      = {
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
                verticalAlignment     = Alignment.Bottom
            ) {
                Column {
                    Text("我的媒体库", color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.3.sp)
                    Text(
                        "视频",
                        color      = TextPrimary,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                }
                LiquidGlassContainer(
                    cornerRadius = 20.dp,
                    isLight      = false,
                    blurRadius   = 14f,
                    onClick      = { showFilterSheet = true }
                ) {
                    Text(
                        text = if (viewModel.currentSort == SortOption.DATE_DESC) "筛选"
                               else viewModel.currentSort.label,
                        color    = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f))
                                .clickable {
                                    selectedTab = tab
                                    if (tab != "文件夹") selectedFolder = null
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(tab, color = GradientStart, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        LiquidGlassContainer(
                            cornerRadius = 50.dp,
                            isLight      = false,
                            blurRadius   = 12f,
                            onClick      = {
                                selectedTab = tab
                                if (tab != "文件夹") selectedFolder = null
                            }
                        ) {
                            Text(
                                text     = tab,
                                color    = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── 新建文件夹按钮（文件夹标签页 + 未进入子文件夹时） ─────────────
            AnimatedVisibility(visible = selectedTab == "文件夹" && selectedFolder == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, glassBorder, RoundedCornerShape(50.dp))
                        .clickable { showNewFolderDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("新建文件夹", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            // ── 内容区 ─────────────────────────────────────────────────────
            when {
                selectedTab == "文件夹" && selectedFolder == null -> {
                    // 文件夹列表
                    if (folders.isEmpty()) {
                        EmptyState(
                            modifier    = Modifier.weight(1f).fillMaxWidth(),
                            selectedTab = selectedTab,
                            onPickFile  = {}
                        )
                    } else {
                        LazyColumn(
                            modifier        = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(folders, key = { it.id }) { folder ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            folderToDelete = folder
                                        }
                                        false // 由对话框确认后才真正删除
                                    }
                                )
                                SwipeToDismissBox(
                                    state             = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {}
                                ) {
                                    FolderCard(
                                        folder  = folder,
                                        onClick = { selectedFolder = folder },
                                        glassBorder = glassBorder
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }

                selectedTab == "文件夹" && selectedFolder != null -> {
                    // 从 folders Flow 实时查找，保证移除视频后立即刷新
                    val folder = folders.find { it.id == selectedFolder!!.id } ?: selectedFolder!!
                    val videosInFolder = folder.videoUris.mapNotNull { uriStr ->
                        videos.find { it.uri.toString() == uriStr }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // 顶栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedFolder = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                            }
                            Text(
                                text       = folder.name,
                                color      = TextPrimary,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                        }

                        if (videosInFolder.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint     = TextSecondary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("文件夹为空", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text("长按视频卡片拖入此文件夹", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier        = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(videosInFolder, key = { it.uri.toString() }) { video ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                videoToRemove = video
                                            }
                                            false // 弹窗确认后才移除，滑动回弹
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state             = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {}
                                    ) {
                                        VideoCard(
                                            video            = video,
                                            savedPositionMs  = savedPositions[video.uri.toString()] ?: 0L,
                                            isFavorite       = favorites.contains(video.uri.toString()),
                                            onFavoriteClick  = { viewModel.toggleFavorite(video.uri) },
                                            onClick          = { onVideoSelected(video.uri) }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }

                else -> {
                    // 全部 / 最近播放 / 收藏 标签页（含长按拖拽支持）
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            userScrollEnabled   = draggingVideo == null
                        ) {
                            items(filteredVideos, key = { it.uri.toString() }) { video ->
                                var cardGlobalOffset by remember { mutableStateOf(Offset.Zero) }

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            videoToHide = video
                                        }
                                        false // 弹窗确认后才隐藏，滑动回弹
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {}
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .onGloballyPositioned { coords ->
                                                cardGlobalOffset = coords.boundsInRoot().topLeft
                                            }
                                            .pointerInput(video.uri) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart  = { localOffset ->
                                                        draggingVideo = video
                                                        dragOffset    = cardGlobalOffset + localOffset
                                                    },
                                                    onDrag       = { _, delta ->
                                                        dragOffset += delta
                                                    },
                                                    onDragEnd    = {
                                                        val hit = folderBoundsMap.entries
                                                            .firstOrNull { (_, rect) -> rect.contains(dragOffset) }
                                                        if (hit != null) {
                                                            viewModel.addVideoToFolder(video.uri, hit.key)
                                                        }
                                                        draggingVideo = null
                                                    },
                                                    onDragCancel = { draggingVideo = null }
                                                )
                                            }
                                    ) {
                                        VideoCard(
                                            video           = video,
                                            savedPositionMs = savedPositions[video.uri.toString()] ?: 0L,
                                            isFavorite      = favorites.contains(video.uri.toString()),
                                            onFavoriteClick = { viewModel.toggleFavorite(video.uri) },
                                            onClick         = { onVideoSelected(video.uri) }
                                        )
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    } // Scaffold

    // ── 拖拽覆盖层 ──────────────────────────────────────────────────────────
    if (draggingVideo != null && folders.isNotEmpty()) {
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
        )

        // Ghost 卡片（跟随手指）
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dragOffset.x - 80).roundToInt(),
                        (dragOffset.y - 40).roundToInt()
                    )
                }
                .width(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard.copy(alpha = 0.92f))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.80f),
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.40f)
                        ),
                        Offset.Zero,
                        Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text     = draggingVideo!!.displayName.substringBeforeLast('.'),
                color    = TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 底部文件夹放置区（按每行 4 个分组，避免实验性 API）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("拖放到文件夹", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            folders.chunked(4).forEach { rowFolders ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowFolders.forEach { folder ->
                        val isHovered = folderBoundsMap[folder.id]?.contains(dragOffset) == true
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    folderBoundsMap[folder.id] = coords.boundsInRoot()
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.80f),
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0.04f),
                                            Color.White.copy(alpha = 0.40f)
                                        ),
                                        Offset.Zero,
                                        Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (isHovered) Color.White.copy(alpha = 0.25f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint     = if (isHovered) TextPrimary else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text     = folder.name,
                                    color    = if (isHovered) TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    } // Box (root)
    } // StarryBackground

    // ── 确认删除文件夹 ─────────────────────────────────────────────────────
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor   = DarkSurface,
            title  = { Text("删除文件夹", color = TextPrimary) },
            text   = { Text("确认删除「${folder.name}」？视频文件不会受影响。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    folderToDelete = null
                }) { Text("删除", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // ── 确认从列表隐藏视频 ─────────────────────────────────────────────────
    videoToHide?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToHide = null },
            containerColor   = DarkSurface,
            title  = { Text("从列表移除", color = TextPrimary) },
            text   = { Text("将「${video.displayName.substringBeforeLast('.')}」从列表中移除？视频文件不会被删除。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideVideo(video.uri)
                    videoToHide = null
                }) { Text("移除", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { videoToHide = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // ── 确认从文件夹移除视频 ───────────────────────────────────────────────
    videoToRemove?.let { video ->
        val currentFolder = selectedFolder
        AlertDialog(
            onDismissRequest = { videoToRemove = null },
            containerColor   = DarkSurface,
            title  = { Text("移除视频", color = TextPrimary) },
            text   = { Text("将「${video.displayName.substringBeforeLast('.')}」从文件夹中移除？视频文件不会被删除。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    if (currentFolder != null) {
                        viewModel.removeVideoFromFolder(video.uri, currentFolder.id)
                    }
                    videoToRemove = null
                }) { Text("移除", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { videoToRemove = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // ── 新建文件夹对话框 ────────────────────────────────────────────────────
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false; newFolderDialogText = "" },
            containerColor   = DarkSurface,
            title  = { Text("新建文件夹", color = TextPrimary) },
            text   = {
                TextField(
                    value         = newFolderDialogText,
                    onValueChange = { newFolderDialogText = it },
                    placeholder   = { Text("文件夹名称", color = TextSecondary) },
                    singleLine    = true,
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        focusedIndicatorColor   = Color.White.copy(alpha = 0.3f),
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                        cursorColor             = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFolder(newFolderDialogText)
                        newFolderDialogText = ""
                        showNewFolderDialog = false
                    },
                    enabled = newFolderDialogText.isNotBlank()
                ) { Text("创建", color = TextPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false; newFolderDialogText = "" }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
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
                            .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
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
                            Icon(
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

// ── 文件夹卡片 ────────────────────────────────────────────────────────────────

@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    glassBorder: Brush
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        // Layer 0: 完全透明基底
        Box(modifier = Modifier.matchParentSize().background(Color.Transparent))

        // Layer 1: 顶部强镜面高光（与 LiquidGlassContainer isLight=false 完全一致）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.65f),
                            0.06f to Color.White.copy(alpha = 0.38f),
                            0.15f to Color.White.copy(alpha = 0.12f),
                            0.28f to Color.White.copy(alpha = 0.02f),
                            1.00f to Color.Transparent
                        )
                    )
                )
        )

        // Layer 2: 左上角散射光斑
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.28f),
                            0.35f to Color.White.copy(alpha = 0.06f),
                            1.00f to Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // Layer 3: 棱边折射描边
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.55f)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = shape
                )
        )

        // Layer 4: 内容
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint     = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = folder.name,
                    color      = TextPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = "${folder.videoUris.size} 个视频",
                    color    = TextSecondary,
                    fontSize = 12.sp
                )
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
        modifier            = modifier,
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
            Icon(
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
                "文件夹"   -> "还没有文件夹"
                else       -> "暂无本地视频"
            },
            color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (selectedTab) {
                "最近播放" -> "开始播放一个视频后会显示在这里"
                "收藏"     -> "长按视频卡片上的♡图标即可收藏"
                "文件夹"   -> "点击上方「新建文件夹」开始分组"
                else       -> "点击下方「＋」按钮选择视频文件"
            },
            color = TextSecondary, fontSize = 14.sp
        )
        if (selectedTab == "全部") {
            Spacer(Modifier.height(28.dp))
            LiquidGlassContainer(
                cornerRadius = 50.dp,
                isLight      = false,
                blurRadius   = 16f,
                onClick      = onPickFile
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.22f), PrimaryBlue.copy(alpha = 0.12f)),
                                start  = Offset(0f, 0f),
                                end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )
                Text(
                    "选择视频",
                    color      = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 28.dp, vertical = 13.dp)
                )
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

    val shape = RoundedCornerShape(16.dp)
    val cardGlassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.55f)
        ),
        start = Offset.Zero,
        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, brush = cardGlassBorder, shape = shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .width(96.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
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
                            modifier     = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF1A2A3C), Color(0xFF0D1826)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.20f))
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
                Icon(
                    imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint     = if (isFavorite) Color(0xFFE53935) else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        // 顶部镜面高光叠层
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.50f),
                            0.08f to Color.White.copy(alpha = 0.20f),
                            0.25f to Color.White.copy(alpha = 0.04f),
                            0.45f to Color.Transparent
                        )
                    )
                )
        )
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
            if (index == 2) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
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
                                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = CircleShape
                            )
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                    Text("添加", color = TextSecondary, fontSize = 10.sp)
                }
            }

            val active = currentRoute == dest.route
            Column(
                modifier = Modifier
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
                Icon(
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
