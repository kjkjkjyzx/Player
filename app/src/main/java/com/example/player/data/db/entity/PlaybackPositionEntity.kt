package com.example.player.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 单个视频的播放进度（毫秒）。替代原 playback_positions SharedPreferences。 */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val uri: String,
    val positionMs: Long
)
