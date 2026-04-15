package com.example.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.ui.components.StarryBackground
import com.example.player.ui.theme.DarkBorder
import com.example.player.ui.theme.DarkSurface
import com.example.player.ui.theme.TextPrimary
import com.example.player.ui.theme.TextSecondary
import com.example.player.viewmodel.HomeViewModel

private val DangerColor = Color(0xFFFF6B6B)

@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }

    StarryBackground(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text       = "我的",
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        // 设置分组 - 数据管理
        SettingGroup(title = "数据管理") {
            SettingItem(
                icon      = Icons.Default.History,
                iconTint  = DangerColor,
                label     = "清除播放历史",
                desc      = "清除所有视频的观看进度",
                onClick   = { dialog = "history" }
            )
            HorizontalDivider(
                modifier  = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color     = DarkBorder
            )
            SettingItem(
                icon      = Icons.Default.FavoriteBorder,
                iconTint  = DangerColor,
                label     = "清除收藏",
                desc      = "清空所有已收藏的视频",
                onClick   = { dialog = "favorites" }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingGroup(title = "关于") {
            SettingItem(
                icon      = Icons.Default.Info,
                iconTint  = TextPrimary,
                label     = "版本信息",
                desc      = "液态玻璃播放器 v1.0.0",
                showArrow = false,
                onClick   = {}
            )
            HorizontalDivider(
                modifier  = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color     = DarkBorder
            )
            SettingItem(
                icon      = Icons.Default.DeleteForever,
                iconTint  = DangerColor,
                label     = "清除缓存",
                desc      = "清除缩略图等本地缓存",
                onClick   = { dialog = "cache" }
            )
        }
    }
    } // StarryBackground

    // 确认弹窗
    dialog?.let { type ->
        val (title, body, action) = when (type) {
            "history"   -> Triple(
                "清除播放历史",
                "将清除所有视频的播放进度，此操作不可撤销。",
                { viewModel.clearPlayHistory() }
            )
            "favorites" -> Triple(
                "清除收藏",
                "将清空所有已收藏的视频，此操作不可撤销。",
                { viewModel.clearFavorites() }
            )
            else        -> Triple(
                "清除缓存",
                "将删除本地缩略图缓存，下次打开列表时会重新生成。",
                { /* Coil 缓存清理可在此处调用 */ }
            )
        }
        AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor   = DarkSurface,
            title            = { Text(title, color = TextPrimary) },
            text             = { Text(body, color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = { action(); dialog = null }) {
                    Text("确认", color = TextPrimary)
                }
            },
            dismissButton    = {
                TextButton(onClick = { dialog = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text     = title,
        color    = TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.55f)
                ),
                    start  = Offset.Zero,
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
        // 顶部镜面高光
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 0.55f),
                            0.06f to Color.White.copy(alpha = 0.22f),
                            0.18f to Color.White.copy(alpha = 0.05f),
                            0.35f to Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    iconTint: Color = TextPrimary,
    label: String,
    desc: String,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = TextSecondary, fontSize = 12.sp)
        }
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint     = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
