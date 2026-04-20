package com.example.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.player.data.db.entity.PlaybackPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackPositionDao {

    @Query("SELECT * FROM playback_positions")
    fun observeAll(): Flow<List<PlaybackPositionEntity>>

    @Query("SELECT positionMs FROM playback_positions WHERE uri = :uri")
    suspend fun getPosition(uri: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackPositionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PlaybackPositionEntity>)

    @Query("DELETE FROM playback_positions")
    suspend fun clear()
}
