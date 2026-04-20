package com.example.player.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 已从主列表隐藏的视频。主键为视频 URI 字符串。 */
@Entity(tableName = "hidden_videos")
data class HiddenVideoEntity(
    @PrimaryKey val uri: String
)
