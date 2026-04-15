package com.example.player.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.player.model.Folder
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository      = VideoRepository(application)
    private val prefs           = application.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)
    private val favPrefs        = application.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val folderPrefs     = application.getSharedPreferences("folders", Context.MODE_PRIVATE)
    private val hiddenPrefs     = application.getSharedPreferences("hidden_videos", Context.MODE_PRIVATE)
    private val importedPrefs   = application.getSharedPreferences("imported_videos", Context.MODE_PRIVATE)

    // 原始未排序列表（来自 MediaStore）
    private val _rawVideos      = MutableStateFlow<List<VideoItem>>(emptyList())

    // 用户通过文件选择器手动导入的视频
    private val _importedVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    // 排序后暴露给 UI 的列表
    private val _videos      = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    /** uri.toString() → 已保存的播放位置（毫秒） */
    private val _savedPositions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val savedPositions: StateFlow<Map<String, Long>> = _savedPositions.asStateFlow()

    /** 收藏集合（存 uri.toString()） */
    private val _favorites   = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    /** 应用内自定义文件夹 */
    private val _folders     = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    /** 已从列表隐藏的视频（存 uri.toString()） */
    private val _hiddenVideos = MutableStateFlow<Set<String>>(emptySet())
    val hiddenVideos: StateFlow<Set<String>> = _hiddenVideos.asStateFlow()

    /** 当前排序方式 */
    var currentSort by mutableStateOf(SortOption.DATE_DESC)
        private set

    init {
        _favorites.value    = favPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
        _hiddenVideos.value = hiddenPrefs.getStringSet("hidden", emptySet()) ?: emptySet()
        loadFolders()
        loadImportedVideos()
    }

    // ── 加载 ────────────────────────────────────────────────────────────────

    fun loadVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.getLocalVideos()
            _rawVideos.value = loaded
            applySortAndFilter()
            _savedPositions.value = loaded.associate { v ->
                v.uri.toString() to prefs.getLong(v.uri.toString(), 0L)
            }
        }
    }

    // ── 排序 ────────────────────────────────────────────────────────────────

    fun setSortOption(option: SortOption) {
        currentSort = option
        applySortAndFilter()
    }

    private fun applySortAndFilter() {
        // 合并 MediaStore 视频与手动导入的视频（去重：以 uri 字符串为 key）
        val mediaStoreUris = _rawVideos.value.map { it.uri.toString() }.toSet()
        val deduped = _rawVideos.value +
                _importedVideos.value.filter { it.uri.toString() !in mediaStoreUris }
        val hidden  = _hiddenVideos.value
        val visible = deduped.filter { it.uri.toString() !in hidden }
        _videos.value = when (currentSort) {
            SortOption.DATE_DESC     -> visible
            SortOption.DATE_ASC      -> visible.reversed()
            SortOption.NAME_ASC      -> visible.sortedBy { it.displayName.lowercase() }
            SortOption.SIZE_DESC     -> visible.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> visible.sortedByDescending { it.duration }
        }
    }

    // ── 手动导入视频 ─────────────────────────────────────────────────────────

    fun importVideos(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUris = _importedVideos.value.map { it.uri.toString() }.toSet()
            val newItems = mutableListOf<VideoItem>()

            for (uri in uris) {
                if (uri.toString() in existingUris) continue
                // 申请持久化 URI 权限，确保重启后仍可访问
                runCatching {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                newItems.add(repository.getVideoMetadata(uri))
            }

            if (newItems.isNotEmpty()) {
                _importedVideos.value = _importedVideos.value + newItems
                saveImportedVideos()
                applySortAndFilter()
            }
        }
    }

    private fun saveImportedVideos() {
        val array = JSONArray()
        _importedVideos.value.forEach { v ->
            val obj = JSONObject()
            obj.put("uri",      v.uri.toString())
            obj.put("name",     v.displayName)
            obj.put("duration", v.duration)
            obj.put("size",     v.size)
            array.put(obj)
        }
        importedPrefs.edit().putString("imported_json", array.toString()).apply()
    }

    private fun loadImportedVideos() {
        val json  = importedPrefs.getString("imported_json", null) ?: return
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return
        val list  = mutableListOf<VideoItem>()
        for (i in 0 until array.length()) {
            val obj      = array.getJSONObject(i)
            val uri      = Uri.parse(obj.getString("uri"))
            val name     = obj.getString("name")
            val duration = obj.getLong("duration")
            val size     = obj.getLong("size")
            list.add(VideoItem(uri, name, duration, size))
        }
        _importedVideos.value = list
        applySortAndFilter()
    }

    // ── 隐藏视频 ─────────────────────────────────────────────────────────────

    fun hideVideo(uri: Uri) {
        val current = _hiddenVideos.value.toMutableSet()
        current.add(uri.toString())
        _hiddenVideos.value = current
        hiddenPrefs.edit().putStringSet("hidden", current).apply()
        applySortAndFilter() // 立即从列表中移除
    }

    // ── 收藏 ────────────────────────────────────────────────────────────────

    fun toggleFavorite(uri: Uri) {
        val key     = uri.toString()
        val current = _favorites.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _favorites.value = current
        favPrefs.edit().putStringSet("favorites", current).apply()
    }

    // ── 文件夹 CRUD ──────────────────────────────────────────────────────────

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val folder  = Folder(id = UUID.randomUUID().toString(), name = trimmed)
        _folders.value = _folders.value + folder
        saveFolders()
    }

    fun deleteFolder(folderId: String) {
        _folders.value = _folders.value.filter { it.id != folderId }
        saveFolders()
    }

    fun addVideoToFolder(videoUri: Uri, folderId: String) {
        val key = videoUri.toString()
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId && !folder.videoUris.contains(key)) {
                folder.copy(videoUris = folder.videoUris + key)
            } else folder
        }
        saveFolders()
    }

    fun removeVideoFromFolder(videoUri: Uri, folderId: String) {
        val key = videoUri.toString()
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(videoUris = folder.videoUris - key)
            else folder
        }
        saveFolders()
    }

    private fun saveFolders() {
        val array = JSONArray()
        _folders.value.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            val uris = JSONArray()
            folder.videoUris.forEach { uris.put(it) }
            obj.put("videoUris", uris)
            array.put(obj)
        }
        folderPrefs.edit().putString("folders_json", array.toString()).apply()
    }

    private fun loadFolders() {
        val json  = folderPrefs.getString("folders_json", null) ?: return
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return
        val list  = mutableListOf<Folder>()
        for (i in 0 until array.length()) {
            val obj    = array.getJSONObject(i)
            val id     = obj.getString("id")
            val name   = obj.getString("name")
            val uriArr = obj.getJSONArray("videoUris")
            val uris   = (0 until uriArr.length()).map { uriArr.getString(it) }
            list += Folder(id = id, name = name, videoUris = uris)
        }
        _folders.value = list
    }

    // ── 工具 ────────────────────────────────────────────────────────────────

    /** 从播放器返回后刷新进度，不重新查询 MediaStore */
    fun refreshPositions() {
        val current = _rawVideos.value
        if (current.isEmpty()) return
        _savedPositions.value = current.associate { v ->
            v.uri.toString() to prefs.getLong(v.uri.toString(), 0L)
        }
    }

    fun getSavedPosition(uri: Uri): Long = prefs.getLong(uri.toString(), 0L)

    fun clearPlayHistory() {
        prefs.edit().clear().apply()
        _savedPositions.value = emptyMap()
    }

    fun clearFavorites() {
        favPrefs.edit().clear().apply()
        _favorites.value = emptySet()
    }
}
