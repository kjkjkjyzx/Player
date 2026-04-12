package com.example.player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.player.model.VideoItem
import com.example.player.repository.VideoRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VideoRepository(application)

    fun loadVideos(): List<VideoItem> = repository.getLocalVideos()
}
