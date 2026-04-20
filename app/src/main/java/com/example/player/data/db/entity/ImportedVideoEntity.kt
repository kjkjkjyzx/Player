package com.example.player.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户通过文件选择器手动导入的视频（URI 已取得 persistable read permission）。
 * 取代原 imported_videos SharedPreferences + JSON。
 */
@Entity(tableName = "imported_videos")
data class ImportedVideoEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val duration: Long,
    val size: Long,
    val isLandscape: Boolean
)
