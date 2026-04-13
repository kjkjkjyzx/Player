package com.example.player.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VideoRepository(application)
    private val prefs = application.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    /** uri.toString() → 已保存的播放位置（毫秒） */
    private val _savedPositions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val savedPositions: StateFlow<Map<String, Long>> = _savedPositions.asStateFlow()

    fun loadVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.getLocalVideos()
            _videos.value = loaded
            _savedPositions.value = loaded.associate { video ->
                video.uri.toString() to prefs.getLong(video.uri.toString(), 0L)
            }
        }
    }

    fun getSavedPosition(uri: Uri): Long = prefs.getLong(uri.toString(), 0L)
}
