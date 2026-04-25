package com.example.player.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import com.example.player.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.ui.components.LiquidGlassContainer
import com.example.player.ui.theme.DarkBackground
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
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
            }
            Text(
                text       = stringResource(R.string.profile_title),
                color      = TextPrimary,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── App 身份头部卡片 ──────────────────────────────────────────────────
        LiquidGlassContainer(
            modifier     = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model              = R.mipmap.ic_launcher_round,
                    contentDescription = null,
                    modifier           = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = stringResource(R.string.app_name),
                    color      = TextPrimary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text     = stringResource(R.string.profile_version_desc, versionName),
                    color    = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 设置分组 - 数据管理
        SettingGroup(title = stringResource(R.string.profile_group_data)) {
            SettingItem(
                icon      = Icons.Default.History,
                iconTint  = DangerColor,
                label     = stringResource(R.string.profile_clear_history),
                desc      = stringResource(R.string.profile_clear_history_desc),
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
                label     = stringResource(R.string.profile_clear_favorites),
                desc      = stringResource(R.string.profile_clear_favorites_desc),
                onClick   = { dialog = "favorites" }
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingGroup(title = stringResource(R.string.profile_group_about)) {
            SettingItem(
                icon      = Icons.Default.Info,
                iconTint  = TextPrimary,
                label     = stringResource(R.string.profile_version),
                desc      = stringResource(R.string.profile_version_desc, versionName),
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
                label     = stringResource(R.string.profile_clear_cache),
                desc      = stringResource(R.string.profile_clear_cache_desc),
                onClick   = { dialog = "cache" }
            )
        }
    }

    // 确认弹窗
    dialog?.let { type ->
        val (title, body, action) = when (type) {
            "history"   -> Triple(
                stringResource(R.string.profile_clear_history),
                stringResource(R.string.profile_clear_history_dialog_msg),
                { viewModel.clearPlayHistory() }
            )
            "favorites" -> Triple(
                stringResource(R.string.profile_clear_favorites),
                stringResource(R.string.profile_clear_favorites_dialog_msg),
                { viewModel.clearFavorites() }
            )
            else        -> Triple(
                stringResource(R.string.profile_clear_cache),
                stringResource(R.string.profile_clear_cache_dialog_msg),
                {}
            )
        }
        AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor   = DarkSurface,
            title            = { Text(title, color = TextPrimary) },
            text             = { Text(body, color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = {
                    if (type == "cache") {
                        viewModel.clearThumbnailCache { ok ->
                            Toast.makeText(
                                context,
                                if (ok) context.getString(R.string.profile_clear_cache_success)
                                else context.getString(R.string.profile_clear_cache_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        action()
                    }
                    dialog = null
                }) {
                    Text(stringResource(R.string.action_confirm), color = TextPrimary)
                }
            },
            dismissButton    = {
                TextButton(onClick = { dialog = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
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
    LiquidGlassContainer(
        modifier     = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
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
