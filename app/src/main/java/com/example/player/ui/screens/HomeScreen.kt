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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.player.model.Folder
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.ui.components.LiquidGlassContainer
import com.example.player.ui.components.StarryBackground
import com.example.player.ui.theme.DarkBackground
import com.example.player.ui.theme.DarkBorder
import com.example.player.ui.theme.DarkSurface
import com.example.player.ui.theme.GradientStart
import com.example.player.ui.theme.DarkCard
import com.example.player.ui.theme.NavBarBg
import com.example.player.ui.theme.PrimaryBlue
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.ui.theme.TextTertiary
import com.example.player.R
import com.example.player.ui.transitions.LocalAnimatedContentScope
import com.example.player.ui.transitions.LocalSharedTransitionScope
import com.example.player.viewmodel.FolderViewModel
import com.example.player.viewmodel.HomeUiState
import com.example.player.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import com.example.player.ui.theme.AppSpring
import com.example.player.ui.theme.GlassDefaults
import com.example.player.ui.transitions.LocalLandscapeExiting

/** 首页四个分类 Tab —— 内部使用 enum 比较，展示用 stringResource 解析 */
private enum class HomeTab(@StringRes val labelRes: Int) {
    ALL(R.string.home_tab_all),
    RECENT(R.string.home_tab_recent),
    FAVORITE(R.string.home_tab_favorite),
    FOLDER(R.string.home_tab_folder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    folderViewModel: FolderViewModel = hiltViewModel(),
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

    val uiState        by viewModel.uiState.collectAsState()
    val videos         by viewModel.videos.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()
    val favorites      by viewModel.favorites.collectAsState()
    val folders        by folderViewModel.folders.collectAsState()
    val currentSort    by viewModel.currentSort.collectAsState()

    var selectedTab         by remember { mutableStateOf(HomeTab.ALL) }
    var showFilterSheet     by remember { mutableStateOf(false) }
    var selectedFolder      by remember { mutableStateOf<Folder?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderDialogText by remember { mutableStateOf("") }
    var folderToDelete      by remember { mutableStateOf<Folder?>(null) }
    var videoToRemove       by remember { mutableStateOf<VideoItem?>(null) }
    var videoToHide         by remember { mutableStateOf<VideoItem?>(null) }

    // 拖拽状态
    var draggingVideo by remember { mutableStateOf<VideoItem?>(null) }
    // 保留 State 引用，供 DragOverlay 在 layout 阶段读取（不触发重组）
    val dragOffsetState = remember { mutableStateOf(Offset.Zero) }
    var dragOffset by dragOffsetState
    // SnapshotStateMap：bounds 写入后可被 derivedStateOf 正确追踪
    val folderBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    val sheetState = rememberModalBottomSheetState()

    // derivedStateOf：仅当过滤结果实际变化时才触发重组，
    // 隔离 savedPositions（播放位置频繁写入）对 HomeScreen 的影响
    val filteredVideos by remember {
        derivedStateOf {
            when (selectedTab) {
                HomeTab.RECENT   -> videos.filter { (savedPositions[it.uri.toString()] ?: 0L) > 0L }
                HomeTab.FAVORITE -> videos.filter { favorites.contains(it.uri.toString()) }
                else             -> videos
            }
        }
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

    StarryBackground(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost        = { },
        containerColor      = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar           = {
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
                    Text(stringResource(R.string.home_library), color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.3.sp)
                    Text(
                        stringResource(R.string.home_videos),
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
                        text = if (currentSort == SortOption.DATE_DESC) stringResource(R.string.home_filter)
                               else stringResource(currentSort.labelRes),
                        color    = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── 分类 Chip 行 ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeTab.entries.forEach { tab ->
                    val selected  = tab == selectedTab
                    val label     = stringResource(tab.labelRes)
                    // iOS 26：弹性选中——背景、边框、文字颜色均通过弹簧物理过渡，非瞬间切换
                    val bgAlpha     by animateFloatAsState(if (selected) 0.92f else 0.06f, AppSpring.standard(), label = "tabBg")
                    val borderAlpha by animateFloatAsState(if (selected) 0.0f  else 0.18f, AppSpring.standard(), label = "tabBorder")
                    val textColor   by animateColorAsState(if (selected) GradientStart else TextPrimary, AppSpring.standard(), label = "tabText")
                    val textWeight  = if (selected) FontWeight.SemiBold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = bgAlpha))
                            .border(0.5.dp, Color.White.copy(alpha = borderAlpha), CircleShape)
                            .clickable {
                                selectedTab = tab
                                if (tab != HomeTab.FOLDER) selectedFolder = null
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(label, color = textColor, fontSize = 12.sp, fontWeight = textWeight)
                    }
                }
            }

            // ── 新建文件夹按钮（文件夹标签页 + 未进入子文件夹时） ─────────────
            AnimatedVisibility(
                visible = selectedTab == HomeTab.FOLDER && selectedFolder == null,
                enter   = expandVertically(AppSpring.standard()) + fadeIn(AppSpring.standard()),
                exit    = shrinkVertically(AppSpring.gentle())   + fadeOut(AppSpring.gentle())
            ) {
                LiquidGlassContainer(
                    modifier     = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    cornerRadius = 50.dp,
                    onClick      = { showNewFolderDialog = true }
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_new_folder), color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            // ── 内容区 ─────────────────────────────────────────────────────
            when {
                selectedTab == HomeTab.FOLDER && selectedFolder == null -> {
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
                            items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
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
                                    modifier          = Modifier.animateItem(
                                        placementSpec = AppSpring.standard(),
                                        fadeInSpec    = AppSpring.gentle(),
                                        fadeOutSpec   = AppSpring.gentle()
                                    ),
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color.Transparent, Color(0xFFE53935).copy(alpha = 0.80f))
                                                    )
                                                ),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint     = Color.White,
                                                modifier = Modifier.padding(end = 20.dp).size(22.dp)
                                            )
                                        }
                                    }
                                ) {
                                    FolderCard(
                                        folder  = folder,
                                        onClick = { selectedFolder = folder }
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }

                selectedTab == HomeTab.FOLDER && selectedFolder != null -> {
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
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
                                    Text(stringResource(R.string.home_folder_empty_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.home_folder_empty_desc), color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier        = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(videosInFolder, key = { it.uri.toString() }, contentType = { "video" }) { video ->
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
                                        modifier          = Modifier.animateItem(
                                            placementSpec = AppSpring.standard(),
                                            fadeInSpec    = AppSpring.gentle(),
                                            fadeOutSpec   = AppSpring.gentle()
                                        ),
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color.Transparent, Color(0xFFE53935).copy(alpha = 0.80f))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    Icons.Default.RemoveCircleOutline,
                                                    contentDescription = null,
                                                    tint     = Color.White,
                                                    modifier = Modifier.padding(end = 20.dp).size(22.dp)
                                                )
                                            }
                                        }
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
                    when {
                        // MediaStore 扫描中且列表为空 → 显示 Loading 指示器
                        uiState is HomeUiState.Loading && filteredVideos.isEmpty() -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color       = Color.White.copy(alpha = 0.55f),
                                    strokeWidth = 2.dp,
                                    modifier    = Modifier.size(32.dp)
                                )
                            }
                        }
                        // 扫描失败且列表为空 → 显示错误 + 重试（委托属性无法智能转换，显式捕获）
                        uiState is HomeUiState.Error && filteredVideos.isEmpty() -> {
                            ErrorState(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                state    = uiState as HomeUiState.Error,
                                onRetry  = { viewModel.retryLoad() }
                            )
                        }
                        filteredVideos.isEmpty() -> {
                            EmptyState(
                                modifier    = Modifier.weight(1f).fillMaxWidth(),
                                selectedTab = selectedTab,
                                onPickFile  = { filePicker.launch(arrayOf("video/*")) }
                            )
                        }
                        else -> {
                            val listState  = rememberLazyListState()
                            val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

                            LazyColumn(
                                state           = listState,
                                modifier        = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                userScrollEnabled   = draggingVideo == null
                            ) {
                                items(filteredVideos, key = { it.uri.toString() }, contentType = { "video" }) { video ->
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
                                        state    = dismissState,
                                        modifier = Modifier.animateItem(
                                            placementSpec = AppSpring.standard(),
                                            fadeInSpec    = AppSpring.gentle(),
                                            fadeOutSpec   = AppSpring.gentle()
                                        ),
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color.Transparent, Color(0xFFE53935).copy(alpha = 0.80f))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint     = Color.White,
                                                    modifier = Modifier.padding(end = 20.dp).size(22.dp)
                                                )
                                            }
                                        }
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
                                                                folderViewModel.addVideoToFolder(video.uri, hit.key)
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
                                                onClick         = { onVideoSelected(video.uri) },
                                                isListScrolling = isScrolling
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
        }
    } // Scaffold

    // ── 拖拽覆盖层 ──────────────────────────────────────────────────────────
    // 独立 Composable：dragOffset 的读取作用域收窄到 DragOverlay 内部，
    // 不再蔓延至 HomeScreen，避免拖拽时 60fps 全屏重组。
    if (draggingVideo != null && folders.isNotEmpty()) {
        DragOverlay(
            draggingVideo   = draggingVideo!!,
            folders         = folders,
            dragOffsetState = dragOffsetState,
            folderBoundsMap = folderBoundsMap
        )
    }

    } // Box (root)
    } // StarryBackground

    // ── 确认删除文件夹 ─────────────────────────────────────────────────────
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor   = DarkSurface,
            title  = { Text(stringResource(R.string.home_delete_folder_title), color = TextPrimary) },
            text   = { Text(stringResource(R.string.home_delete_folder_desc, folder.name), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    folderViewModel.deleteFolder(folder.id)
                    folderToDelete = null
                }) { Text(stringResource(R.string.action_delete), color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }

    // ── 确认从列表隐藏视频 ─────────────────────────────────────────────────
    videoToHide?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToHide = null },
            containerColor   = DarkSurface,
            title  = { Text(stringResource(R.string.home_hide_video_title), color = TextPrimary) },
            text   = { Text(stringResource(R.string.home_hide_video_desc, video.displayName.substringBeforeLast('.')), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideVideo(video.uri)
                    videoToHide = null
                }) { Text(stringResource(R.string.action_remove), color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { videoToHide = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
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
            title  = { Text(stringResource(R.string.home_remove_from_folder_title), color = TextPrimary) },
            text   = { Text(stringResource(R.string.home_remove_from_folder_desc, video.displayName.substringBeforeLast('.')), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    if (currentFolder != null) {
                        folderViewModel.removeVideoFromFolder(video.uri, currentFolder.id)
                    }
                    videoToRemove = null
                }) { Text(stringResource(R.string.action_remove), color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { videoToRemove = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }

    // ── 新建文件夹对话框 ────────────────────────────────────────────────────
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false; newFolderDialogText = "" },
            containerColor   = DarkSurface,
            title  = { Text(stringResource(R.string.home_create_folder_title), color = TextPrimary) },
            text   = {
                TextField(
                    value         = newFolderDialogText,
                    onValueChange = { newFolderDialogText = it },
                    placeholder   = { Text(stringResource(R.string.home_folder_name_placeholder), color = TextSecondary) },
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
                        folderViewModel.createFolder(newFolderDialogText)
                        newFolderDialogText = ""
                        showNewFolderDialog = false
                    },
                    enabled = newFolderDialogText.isNotBlank()
                ) { Text(stringResource(R.string.action_create), color = TextPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false; newFolderDialogText = "" }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
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
                Text(stringResource(R.string.home_sort), color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(12.dp))
                SortOption.entries.forEach { option ->
                    val selected = option == currentSort
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
                        Text(stringResource(option.labelRes), color = TextPrimary, fontSize = 15.sp)
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

// Variant A 极简风格：磨砂基底 + 单色细描边，移除高功耗高光/散射层
@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit
) {
    LiquidGlassContainer(
        modifier     = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick      = onClick
    ) {
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
                    text     = stringResource(R.string.home_folder_video_count, folder.videoUris.size),
                    color    = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── 拖拽覆盖层 ────────────────────────────────────────────────────────────────
//
// 将 dragOffset 读取作用域收窄到此 Composable 内部：
//   • Ghost 卡片位置：Modifier.offset{} 在 Layout 阶段读取，不触发重组
//   • 文件夹悬停：derivedStateOf 计算 hoveredFolderId，仅当悬停目标切换时才重组

@Composable
private fun DragOverlay(
    draggingVideo   : VideoItem,
    folders         : List<com.example.player.model.Folder>,
    dragOffsetState : State<Offset>,
    folderBoundsMap : MutableMap<String, Rect>
) {
    // 仅当悬停的文件夹 id 实际改变时才重组，屏蔽每帧的坐标变化
    val hoveredFolderId by remember {
        derivedStateOf {
            val offset = dragOffsetState.value
            folderBoundsMap.entries.firstOrNull { (_, rect) -> rect.contains(offset) }?.key
        }
    }

    // 包裹 Box：提供 BoxScope，使 Ghost 卡片和 Column 可以使用 Modifier.align
    Box(modifier = Modifier.fillMaxSize()) {

    // 半透明遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
    )

    // Ghost 卡片（跟随手指）
    // Modifier.offset{} 是 lambda 形式，在 Layout 阶段读取 state，不触发重组
    Box(
        modifier = Modifier
            .offset {
                val o = dragOffsetState.value
                IntOffset((o.x - 80).roundToInt(), (o.y - 40).roundToInt())
            }
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard.copy(alpha = 0.92f))
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text     = draggingVideo.displayName.substringBeforeLast('.'),
            color    = TextPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // 底部文件夹放置区（按每行 4 个分组）
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.home_drop_target), color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        folders.chunked(4).forEach { rowFolders ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowFolders.forEach { folder ->
                    // 只读 hoveredFolderId，仅在悬停切换时重组（非每帧）
                    val isHovered = hoveredFolderId == folder.id
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                folderBoundsMap[folder.id] = coords.boundsInRoot()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = if (isHovered) 0.38f else 0.18f),
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
    } // Box wrapper（提供 BoxScope for Modifier.align）
}

// ── 空状态 ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    selectedTab: HomeTab,
    onPickFile: () -> Unit
) {
    val titleRes = when (selectedTab) {
        HomeTab.RECENT   -> R.string.empty_recent_title
        HomeTab.FAVORITE -> R.string.empty_favorite_title
        HomeTab.FOLDER   -> R.string.empty_folders_title
        HomeTab.ALL      -> R.string.empty_all_title
    }
    val descRes = when (selectedTab) {
        HomeTab.RECENT   -> R.string.empty_recent_desc
        HomeTab.FAVORITE -> R.string.empty_favorite_desc
        HomeTab.FOLDER   -> R.string.empty_folders_desc
        HomeTab.ALL      -> R.string.empty_all_desc
    }
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
            text = stringResource(titleRes),
            color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(descRes),
            color = TextSecondary, fontSize = 14.sp
        )
        if (selectedTab == HomeTab.ALL) {
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
                    stringResource(R.string.home_pick_video),
                    color      = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 28.dp, vertical = 13.dp)
                )
            }
        }
    }
}

// ── 错误态 ──────────────────────────────────────────────────────────────────

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    state: HomeUiState.Error,
    onRetry: () -> Unit
) {
    val title = when (state.reason) {
        HomeUiState.Error.Reason.PERMISSION_DENIED -> stringResource(R.string.error_permission_title)
        HomeUiState.Error.Reason.IO                -> stringResource(R.string.error_io_title)
        HomeUiState.Error.Reason.UNKNOWN           -> stringResource(R.string.error_unknown_title)
    }
    val hint = when (state.reason) {
        HomeUiState.Error.Reason.PERMISSION_DENIED -> stringResource(R.string.error_permission_desc)
        HomeUiState.Error.Reason.IO                -> state.message ?: stringResource(R.string.error_io_desc_fallback)
        HomeUiState.Error.Reason.UNKNOWN           -> state.message ?: stringResource(R.string.error_unknown_desc_fallback)
    }
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFE57373).copy(alpha = 0.25f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint     = Color(0xFFE57373),
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(title,  color = TextPrimary,   fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(hint,   color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        LiquidGlassContainer(
            cornerRadius = 50.dp,
            isLight      = false,
            blurRadius   = 16f,
            onClick      = onRetry
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
                stringResource(R.string.action_retry),
                color      = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 28.dp, vertical = 13.dp)
            )
        }
    }
}

// ── 视频卡片 ──────────────────────────────────────────────────────────────────

// Variant A 极简风格：单色 0.5dp 描边，移除高光叠层；滚动时仅读内存缓存避免 VideoFrameDecoder 占用 CPU
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun VideoCard(
    video: VideoItem,
    savedPositionMs: Long,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    isListScrolling: Boolean = false,  // 高速滚动时跳过视频帧解码
    modifier: Modifier = Modifier      // 用于 LazyColumn.animateItem() 等外部 modifier
) {
    val context  = LocalContext.current
    val progress = if (video.duration > 0L) savedPositionMs.toFloat() / video.duration else 0f

    val sharedScope   = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedContentScope.current

    // sharedBounds：缩略图 ↔ PlayerScreen 全屏做位置/尺寸/形状插值动画
    // 进入横屏视频时禁用（坐标系翻转），退出时由 landscapeExiting=true 临时开启。
    val landscapeExiting = LocalLandscapeExiting.current
    @Suppress("NAME_SHADOWING")
    val sharedModifier: Modifier = if (sharedScope != null && animatedScope != null &&
        (!video.isLandscape || landscapeExiting)) {
        with(sharedScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "player-container-${video.uri}"),
                animatedVisibilityScope = animatedScope,
                // 返回时快速淡入缩略图，配合容器收缩动画
                enter = fadeIn(tween(200)),
                // 前进时保持缩略图可见 500ms，让图片在容器展开过程中始终清晰，
                // 直至 PlayerScreen 黑色背景（160ms delay）接管
                exit  = fadeOut(tween(durationMillis = 500)),
                // 对应缩略图的 10dp 圆角（而非卡片的 16dp）
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(10.dp))
            )
        }
    } else Modifier

    // iOS 26 按压缩放反馈：手指按下时卡片轻微缩小（0.965x），松开弹回
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val cardScale         by animateFloatAsState(
        targetValue   = if (isPressed) 0.965f else 1.0f,
        animationSpec = AppSpring.press(),
        label         = "cardScale"
    )

    // 收藏图标颜色弹簧过渡（iOS：状态切换时颜色弹性变换，非瞬间切换）
    val heartColor by animateColorAsState(
        targetValue   = if (isFavorite) Color(0xFFE53935) else TextSecondary,
        animationSpec = AppSpring.press(),
        label         = "heartColor"
    )

    val shape = RoundedCornerShape(16.dp)

    // 外层 Box：应用外部传入的 modifier（如 animateItem），再叠加 press scale
    Box(modifier = modifier.fillMaxWidth().scale(cardScale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = GlassDefaults.elevation,
                    shape        = shape,
                    clip         = false,
                    spotColor    = Color.Black.copy(alpha = 0.20f),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                // sharedBounds 已移至缩略图，此处仅保留卡片视觉样式
                .clip(shape)
                // 渐变填充 + 顶部高光 + 底部反射合并到 drawBehind，消除独立 Canvas 节点
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
                .border(GlassDefaults.borderWidth, GlassDefaults.borderBrush, shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图：sharedBounds 放在 clip() 之前，让缩略图本体从 96×72dp 直接展开到全屏
            // 过渡期间其他卡片内容（文字、按钮）随 HomeScreen 整体淡出，不参与空间变换
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .width(96.dp)
                    .height(72.dp)
                    .then(sharedModifier)      // ← 移至此处
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 缩略图优先：磁盘预生成 JPEG (ThumbnailWorker) → 无则回退 VideoFrameDecoder
                val cachedThumb = remember(video.uri) {
                    com.example.player.data.thumbs.ThumbnailCache.existing(context, video.uri.toString())
                }
                val imageData: Any = cachedThumb ?: video.uri
                // remember：isListScrolling 每次滚动仅切换 2 次，缓存避免每次重组都 new ImageRequest
                val imageModel = remember(imageData, isListScrolling) {
                    if (isListScrolling) {
                        ImageRequest.Builder(context)
                            .data(imageData)
                            .memoryCachePolicy(CachePolicy.READ_ONLY)
                            .build()
                    } else {
                        ImageRequest.Builder(context)
                            .data(imageData)
                            .setParameter("coil#video_frame_micros", 1_000_000L)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                }

                // 占位背景（静态，图片加载完成后被 AsyncImage 覆盖）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = null,
                        tint     = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                // AsyncImage 替换 SubcomposeAsyncImage：消除每个 item 的子组合开销
                AsyncImage(
                    model              = imageModel,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
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
                    maxLines   = 2,
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
                    text     = if (savedPositionMs > 0L) stringResource(R.string.card_last_position, formatTime(savedPositionMs))
                               else stringResource(R.string.card_not_played),
                    color    = TextTertiary,
                    fontSize = 11.sp
                )
            }

            // 收藏按钮（弹性颜色过渡，heartColor 由 animateColorAsState 驱动）
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.cd_unfavorite) else stringResource(R.string.cd_favorite),
                    tint     = heartColor,   // 弹簧颜色过渡
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        }   // 关闭内层 Box（sharedBounds 层）
    }   // 关闭外层 Box（press scale 层）
}   // VideoCard 函数结束

// ── 底部导航栏 ────────────────────────────────────────────────────────────────

private data class NavDest(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
)

@Composable
fun BottomNavBar(
    navController: NavController,
    onAddClick: () -> Unit
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute  = currentEntry?.destination?.route

    val items = listOf(
        NavDest("home",    Icons.Default.VideoLibrary, R.string.nav_library),
        NavDest("search",  Icons.Default.Search,       R.string.nav_search),
        NavDest("list",    Icons.Default.Queue,        R.string.nav_list),
        NavDest("profile", Icons.Default.Person,       R.string.nav_profile)
    )

    // 浮动药丸容器：填满宽度 + 系统导航条适配 + 内边距
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassContainer(
            modifier     = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, dest ->
                    // ── 导入（"+"）按钮插在 index=2 之前 ──────────────────────
                    if (index == 2) {
                        val addInteraction = remember { MutableInteractionSource() }
                        val addPressed by addInteraction.collectIsPressedAsState()
                        val addScale by animateFloatAsState(
                            targetValue   = if (addPressed) 0.88f else 1.0f,
                            animationSpec = AppSpring.press(),
                            label         = "addScale"
                        )
                        Column(
                            modifier            = Modifier.scale(addScale),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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
                                    .clickable(
                                        interactionSource = addInteraction,
                                        indication        = null,
                                        onClick           = onAddClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.cd_add),
                                    tint     = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                stringResource(R.string.nav_import),
                                color    = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // ── 常规导航项 ────────────────────────────────────────────
                    val active    = currentRoute == dest.route
                    val destLabel = stringResource(dest.labelRes)

                    val navInteraction = remember { MutableInteractionSource() }
                    val navPressed by navInteraction.collectIsPressedAsState()
                    val navScale by animateFloatAsState(
                        targetValue   = if (navPressed) 0.88f else 1.0f,
                        animationSpec = AppSpring.press(),
                        label         = "navScale${dest.route}"
                    )
                    val iconTint by animateColorAsState(
                        targetValue   = if (active) TextPrimary else TextSecondary.copy(alpha = 0.60f),
                        animationSpec = AppSpring.standard(),
                        label         = "navTint${dest.route}"
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue   = if (active) 1f else 0f,
                        animationSpec = AppSpring.standard(),
                        label         = "navDot${dest.route}"
                    )

                    Column(
                        modifier = Modifier
                            .scale(navScale)
                            .clickable(
                                interactionSource = navInteraction,
                                indication        = null
                            ) {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            dest.icon,
                            contentDescription = destLabel,
                            tint     = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        // 选中指示点（直径 4dp，弹性淡入淡出）
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    GradientStart.copy(alpha = dotAlpha),
                                    CircleShape
                                )
                        )
                        Text(destLabel, color = iconTint, fontSize = 10.sp)
                    }
                }
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
