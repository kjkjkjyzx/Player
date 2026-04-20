package com.example.player.di

import android.content.Context
import androidx.room.Room
import com.example.player.data.datastore.SettingsDataStore
import com.example.player.data.db.PlayerDatabase
import com.example.player.data.db.dao.FavoriteDao
import com.example.player.data.db.dao.FolderDao
import com.example.player.data.db.dao.HiddenVideoDao
import com.example.player.data.db.dao.ImportedVideoDao
import com.example.player.data.db.dao.PlaybackPositionDao
import com.example.player.repository.MediaStoreVideoRepository
import com.example.player.repository.VideoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 单例作用域的核心依赖装配：数据库、DAO、仓库、Preferences。
 * 拆模块维度保持最小：先用一个 AppModule 覆盖全部，后期再按 data / repo / vm 维度切分。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePlayerDatabase(@ApplicationContext ctx: Context): PlayerDatabase =
        Room.databaseBuilder(ctx, PlayerDatabase::class.java, PlayerDatabase.NAME)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun provideFavoriteDao(db: PlayerDatabase): FavoriteDao                 = db.favoriteDao()
    @Provides fun provideHiddenVideoDao(db: PlayerDatabase): HiddenVideoDao           = db.hiddenVideoDao()
    @Provides fun provideImportedVideoDao(db: PlayerDatabase): ImportedVideoDao       = db.importedVideoDao()
    @Provides fun providePlaybackPositionDao(db: PlayerDatabase): PlaybackPositionDao = db.playbackPositionDao()
    @Provides fun provideFolderDao(db: PlayerDatabase): FolderDao                     = db.folderDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext ctx: Context): SettingsDataStore =
        SettingsDataStore(ctx)

    @Provides
    @Singleton
    fun provideVideoRepository(@ApplicationContext ctx: Context): VideoRepository =
        MediaStoreVideoRepository(ctx)
}
