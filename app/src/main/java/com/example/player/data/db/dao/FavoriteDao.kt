package com.example.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.player.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT uri FROM favorites")
    fun observeUris(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(entities: List<FavoriteEntity>)
}
