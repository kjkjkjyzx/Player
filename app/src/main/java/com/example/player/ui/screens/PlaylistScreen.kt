package com.example.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.ui.theme.DarkBackground
import com.example.player.ui.theme.DarkCard
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.viewmodel.HomeViewModel

@Composable
fun PlaylistScreen(
    viewModel: HomeViewModel,
    onVideoSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val videos         by viewModel.videos.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()
    val favorites      by viewModel.favorites.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    // 有播放记录的视频（按文件顺序）
    val history = videos.filter { (savedPositions[it.uri.toString()] ?: 0L) > 0L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(
                text       = "最近播放",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f)
            )
            if (history.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清除记录", tint = TextSecondary)
                }
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.06f), Color.Transparent)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Queue, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("暂无播放记录", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("播放过的视频会显示在这里", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            Text(
                text     = "共 ${history.size} 个视频",
                color    = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.uri.toString() }) { video ->
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
    }

    // 清除确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = DarkCard,
            title            = { Text("清除播放记录", color = TextPrimary) },
            text             = { Text("将清除所有视频的播放进度，此操作不可撤销。", color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.clearPlayHistory()
                    showClearDialog = false
                }) {
                    Text("清除", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}
