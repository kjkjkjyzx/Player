package com.example.player.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("playback_positions", Context.MODE_PRIVATE)

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    var isPlaying by mutableStateOf(false)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var currentPosition by mutableLongStateOf(0L)
        private set
    var controlsVisible by mutableStateOf(true)
        private set
    var videoTitle by mutableStateOf("")
        private set
    var isReady by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var isLandscapeVideo by mutableStateOf(false)
        private set
    var videoSizeKnown by mutableStateOf(false)
        private set

    private var hideControlsJob: Job? = null
    private var initialized = false
    private var positionRestored = false
    private var currentUri: Uri? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    scheduleHideControls()
                } else {
                    hideControlsJob?.cancel()
                    controlsVisible = true
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    isReady = true
                    duration = player.duration.coerceAtLeast(0L)
                    if (!positionRestored) {
                        positionRestored = true
                        val saved = currentUri?.let { prefs.getLong(it.toString(), 0L) } ?: 0L
                        // 超过 2 秒才恢复，避免从片头播放也跳转
                        if (saved > 2000L) player.seekTo(saved)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    isLandscapeVideo = videoSize.width >= videoSize.height
                    videoSizeKnown = true
                }
            }
        })

        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                }
                delay(200)
            }
        }
    }

    fun initializePlayer(uri: Uri) {
        if (initialized) return
        initialized = true
        currentUri = uri
        videoTitle = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?: "Video"
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
        showControls()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, duration))
        currentPosition = positionMs.coerceIn(0L, duration)
    }

    fun seekBy(deltaMs: Long) {
        seekTo(currentPosition + deltaMs)
        showControls()
    }

    fun showControls() {
        controlsVisible = true
        scheduleHideControls()
    }

    fun toggleControls() {
        if (controlsVisible) {
            hideControlsJob?.cancel()
            controlsVisible = false
        } else {
            showControls()
        }
    }

    fun onPause() {
        savePosition()
        player.pause()
    }

    fun onResume() { player.play() }

    private fun savePosition() {
        currentUri?.let { uri ->
            prefs.edit().putLong(uri.toString(), player.currentPosition).apply()
        }
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        if (player.isPlaying) {
            hideControlsJob = viewModelScope.launch {
                delay(3000)
                controlsVisible = false
            }
        }
    }

    override fun onCleared() {
        savePosition()
        player.release()
        super.onCleared()
    }
}
