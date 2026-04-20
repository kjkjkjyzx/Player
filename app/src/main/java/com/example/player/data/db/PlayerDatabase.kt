package com.example.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.player.data.db.dao.FavoriteDao
import com.example.player.data.db.dao.FolderDao
import com.example.player.data.db.dao.HiddenVideoDao
import com.example.player.data.db.dao.ImportedVideoDao
import com.example.player.data.db.dao.PlaybackPositionDao
import com.example.player.data.db.entity.FavoriteEntity
import com.example.player.data.db.entity.FolderEntity
import com.example.player.data.db.entity.FolderVideoCrossRef
import com.example.player.data.db.entity.HiddenVideoEntity
import com.example.player.data.db.entity.ImportedVideoEntity
import com.example.player.data.db.entity.PlaybackPositionEntity

/**
 * 应用唯一 Room 数据库。
 *
 * 版本策略：
 *  v1 — 初始 schema（favorites / hidden_videos / imported_videos / playback_positions / folders / folder_videos）
 *  未来加列 / 加表用 Migration，不要 fallbackToDestructiveMigration 到线上。
 */
@Database(
    entities = [
        FavoriteEntity::class,
        HiddenVideoEntity::class,
        ImportedVideoEntity::class,
        PlaybackPositionEntity::class,
        FolderEntity::class,
        FolderVideoCrossRef::class
    ],
    version           = 1,
    exportSchema      = true
)
abstract class PlayerDatabase : RoomDatabase() {
    abstract fun favoriteDao():         FavoriteDao
    abstract fun hiddenVideoDao():      HiddenVideoDao
    abstract fun importedVideoDao():    ImportedVideoDao
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun folderDao():           FolderDao

    companion object {
        const val NAME = "player.db"
    }
}
