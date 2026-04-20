package com.example.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.player.data.db.entity.ImportedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedVideoDao {

    @Query("SELECT * FROM imported_videos")
    fun observeAll(): Flow<List<ImportedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ImportedVideoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ImportedVideoEntity>)

    @Query("DELETE FROM imported_videos WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("DELETE FROM imported_videos")
    suspend fun clear()
}
