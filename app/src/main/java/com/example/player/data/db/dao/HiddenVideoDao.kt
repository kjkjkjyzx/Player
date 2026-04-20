package com.example.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.player.data.db.entity.HiddenVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenVideoDao {

    @Query("SELECT uri FROM hidden_videos")
    fun observeUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: HiddenVideoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(entities: List<HiddenVideoEntity>)

    @Query("DELETE FROM hidden_videos WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("DELETE FROM hidden_videos")
    suspend fun clear()
}
