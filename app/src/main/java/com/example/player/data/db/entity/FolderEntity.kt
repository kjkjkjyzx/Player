package com.example.player.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 应用内自定义文件夹。 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String
)

/**
 * 文件夹↔视频多对多关系表。用 (folderId, videoUri) 复合主键，
 * 并对 folderId 建索引以加速联表查询（Room 会在外键上强制要求索引）。
 */
@Entity(
    tableName      = "folder_videos",
    primaryKeys    = ["folderId", "videoUri"],
    foreignKeys    = [
        ForeignKey(
            entity        = FolderEntity::class,
            parentColumns = ["id"],
            childColumns  = ["folderId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class FolderVideoCrossRef(
    val folderId: String,
    val videoUri: String
)
