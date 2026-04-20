package com.example.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.player.data.db.entity.FolderEntity
import com.example.player.data.db.entity.FolderVideoCrossRef
import kotlinx.coroutines.flow.Flow

/** 扁平联表查询结果：每行一个 (folderId, folderName, videoUri?)。在仓库层折叠为领域模型。 */
data class FolderWithVideoRow(
    val folderId: String,
    val folderName: String,
    val videoUri: String?
)

@Dao
interface FolderDao {

    /**
     * LEFT JOIN 保证空文件夹也会出现一行（videoUri 为 NULL）。
     * 按文件夹名 + URI 排序以保持稳定呈现。
     */
    @Query(
        """
        SELECT f.id AS folderId, f.name AS folderName, fv.videoUri AS videoUri
        FROM folders f
        LEFT JOIN folder_videos fv ON fv.folderId = f.id
        ORDER BY f.name COLLATE NOCASE ASC, fv.videoUri ASC
        """
    )
    fun observeFoldersWithVideos(): Flow<List<FolderWithVideoRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRef(ref: FolderVideoCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRefs(refs: List<FolderVideoCrossRef>)

    @Query("DELETE FROM folder_videos WHERE folderId = :folderId AND videoUri = :videoUri")
    suspend fun removeCrossRef(folderId: String, videoUri: String)

    @Transaction
    suspend fun upsertFolderWithVideos(folder: FolderEntity, videoUris: List<String>) {
        insertFolder(folder)
        addCrossRefs(videoUris.map { FolderVideoCrossRef(folder.id, it) })
    }
}
