package com.example.player.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 收藏视频。主键为视频 URI 字符串。 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val uri: String
)
