package com.example.player.data.migration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.example.player.data.db.PlayerDatabase
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

private val Context.migrationFlags: DataStore<Preferences> by preferencesDataStore(name = "migration_flags")

/**
 * 一次性把旧版 SharedPreferences / JSON 数据搬入 Room。
 *
 * 完成标志写入独立的 `migration_flags.preferences_pb` DataStore，避免重复跑。
 * 旧 SharedPreferences 文件在迁移成功后清空（但不删文件，避免 FileObserver 问题）。
 */
@Singleton
class LegacyPrefsMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db:            PlayerDatabase,
    private val favoriteDao:   FavoriteDao,
    private val hiddenDao:     HiddenVideoDao,
    private val importedDao:   ImportedVideoDao,
    private val positionDao:   PlaybackPositionDao,
    private val folderDao:     FolderDao
) {

    private val doneKey = booleanPreferencesKey("legacy_prefs_migrated_v1")

    suspend fun migrateIfNeeded() {
        val already = context.migrationFlags.data.first()[doneKey] ?: false
        if (already) return

        // 幂等保护：若 Room 中已存在任意数据，认为迁移已完成并补写标记，避免重复插入。
        if (hasRoomData()) {
            context.migrationFlags.edit { it[doneKey] = true }
            return
        }

        val payload = loadLegacyPayload()

        db.withTransaction {
            if (payload.favorites.isNotEmpty()) {
                favoriteDao.addAll(payload.favorites.map { FavoriteEntity(it) })
            }
            if (payload.hidden.isNotEmpty()) {
                hiddenDao.addAll(payload.hidden.map { HiddenVideoEntity(it) })
            }
            if (payload.positions.isNotEmpty()) {
                positionDao.upsertAll(payload.positions)
            }
            if (payload.imported.isNotEmpty()) {
                importedDao.insertAll(payload.imported)
            }
            payload.folders.forEach { folder ->
                folderDao.insertFolder(FolderEntity(id = folder.id, name = folder.name))
                if (folder.videoUris.isNotEmpty()) {
                    folderDao.addCrossRefs(folder.videoUris.map { FolderVideoCrossRef(folder.id, it) })
                }
            }
        }

        clearLegacyStores()

        context.migrationFlags.edit { it[doneKey] = true }
    }

    private suspend fun hasRoomData(): Boolean {
        return favoriteDao.observeUris().first().isNotEmpty() ||
            hiddenDao.observeUris().first().isNotEmpty() ||
            importedDao.observeAll().first().isNotEmpty() ||
            positionDao.observeAll().first().isNotEmpty() ||
            folderDao.observeFoldersWithVideos().first().isNotEmpty()
    }

    private fun loadLegacyPayload(): LegacyPayload {
        val favorites = readFavorites()
        val hidden = readHidden()
        val positions = readPositions()
        val imported = readImported()
        val folders = readFolders()
        return LegacyPayload(
            favorites = favorites,
            hidden = hidden,
            positions = positions,
            imported = imported,
            folders = folders
        )
    }

    private fun readFavorites(): Set<String> {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    private fun readHidden(): Set<String> {
        val prefs = context.getSharedPreferences("hidden_videos", Context.MODE_PRIVATE)
        return prefs.getStringSet("hidden", emptySet()) ?: emptySet()
    }

    private fun readPositions(): List<PlaybackPositionEntity> {
        val prefs = context.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (uri, v) ->
            val ms = (v as? Long) ?: return@mapNotNull null
            if (ms <= 0L) null else PlaybackPositionEntity(uri, ms)
        }
    }

    private fun readImported(): List<ImportedVideoEntity> {
        val prefs = context.getSharedPreferences("imported_videos", Context.MODE_PRIVATE)
        val json  = prefs.getString("imported_json", null) ?: return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val list  = mutableListOf<ImportedVideoEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list += ImportedVideoEntity(
                uri         = obj.getString("uri"),
                displayName = obj.getString("name"),
                duration    = obj.getLong("duration"),
                size        = obj.getLong("size"),
                isLandscape = runCatching { obj.getBoolean("landscape") }.getOrDefault(false)
            )
        }
        return list
    }

    private fun readFolders(): List<LegacyFolderPayload> {
        val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)
        val json  = prefs.getString("folders_json", null) ?: return emptyList()
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val folders = mutableListOf<LegacyFolderPayload>()
        for (i in 0 until array.length()) {
            val obj    = array.getJSONObject(i)
            val id     = obj.getString("id")
            val name   = obj.getString("name")
            val uriArr = obj.getJSONArray("videoUris")
            val uris   = (0 until uriArr.length()).map { uriArr.getString(it) }
            folders += LegacyFolderPayload(id = id, name = name, videoUris = uris)
        }
        return folders
    }

    private fun clearLegacyStores() {
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("hidden_videos", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("playback_positions", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("imported_videos", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("folders", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private data class LegacyFolderPayload(
        val id: String,
        val name: String,
        val videoUris: List<String>
    )

    private data class LegacyPayload(
        val favorites: Set<String>,
        val hidden: Set<String>,
        val positions: List<PlaybackPositionEntity>,
        val imported: List<ImportedVideoEntity>,
        val folders: List<LegacyFolderPayload>
    )
}
