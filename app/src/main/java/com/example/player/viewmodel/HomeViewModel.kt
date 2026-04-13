package com.example.player.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.player.model.SortOption
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository  = VideoRepository(application)
    private val prefs       = application.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)
    private val favPrefs    = application.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    // 原始未排序列表（来自 MediaStore）
    private val _rawVideos  = MutableStateFlow<List<VideoItem>>(emptyList())

    // 排序后暴露给 UI 的列表
    private val _videos     = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    /** uri.toString() → 已保存的播放位置（毫秒） */
    private val _savedPositions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val savedPositions: StateFlow<Map<String, Long>> = _savedPositions.asStateFlow()

    /** 收藏集合（存 uri.toString()） */
    private val _favorites  = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    /** 当前排序方式 */
    var currentSort by mutableStateOf(SortOption.DATE_DESC)
        private set

    init {
        _favorites.value = favPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
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
        val raw = _rawVideos.value
        _videos.value = when (currentSort) {
            SortOption.DATE_DESC     -> raw
            SortOption.DATE_ASC      -> raw.reversed()
            SortOption.NAME_ASC      -> raw.sortedBy { it.displayName.lowercase() }
            SortOption.SIZE_DESC     -> raw.sortedByDescending { it.size }
            SortOption.DURATION_DESC -> raw.sortedByDescending { it.duration }
        }
    }

    // ── 收藏 ────────────────────────────────────────────────────────────────

    fun toggleFavorite(uri: Uri) {
        val key     = uri.toString()
        val current = _favorites.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _favorites.value = current
        favPrefs.edit().putStringSet("favorites", current).apply()
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
