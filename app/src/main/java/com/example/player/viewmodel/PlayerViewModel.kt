package com.example.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.player.data.db.dao.PlaybackPositionDao
import com.example.player.data.db.entity.PlaybackPositionEntity
import com.example.player.data.subtitle.SubtitleResolver
import com.example.player.service.PlayerService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val positionDao: PlaybackPositionDao
) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var connectionTimeoutJob: Job? = null

    private val _connectionError = MutableStateFlow(false)
    val connectionError: StateFlow<Boolean> = _connectionError.asStateFlow()

    // 指数退避：初始 1s，最大 16s，连接成功后重置
    private var currentRetryDelayMs = 1_000L
    private val maxRetryDelayMs = 16_000L

    var player: Player? by mutableStateOf(null)
        private set

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

    var videoWidth by mutableIntStateOf(0)
        private set
    var videoHeight by mutableIntStateOf(0)
        private set

    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set

    var volumePercent by mutableIntStateOf(
        run {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) (cur * 100f / max).roundToInt() else 50
        }
    )
        private set

    var brightnessPercent by mutableIntStateOf(50)
        private set

    private var hideControlsJob: Job? = null
    private var initialized = false
    private var initJob: Job? = null
    private var positionRestored = false
    private var currentUri: Uri? = null
    private var pendingUri: Uri? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            if (playing) scheduleHideControls()
            else {
                hideControlsJob?.cancel()
                controlsVisible = true
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            val p = player ?: return
            isBuffering = state == Player.STATE_BUFFERING
            if (state == Player.STATE_READY) {
                isReady = true
                duration = p.duration.coerceAtLeast(0L)
                if (!positionRestored) {
                    positionRestored = true
                    val key = currentUri?.toString() ?: return
                    viewModelScope.launch {
                        val saved = positionDao.getPosition(key) ?: 0L
                        if (saved > 2000L) p.seekTo(saved)
                    }
                }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                isLandscapeVideo = videoSize.width >= videoSize.height
                videoSizeKnown = true
            }
        }
    }

    init {
        connectController()

        viewModelScope.launch {
            while (true) {
                val p = player
                if (p != null && p.isPlaying) {
                    currentPosition = p.currentPosition.coerceAtLeast(0L)
                }
                delay(200)
            }
        }
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlayerService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        _connectionError.value = false

        future.addListener(
            {
                try {
                    val controller = future.get()
                    controller.addListener(listener)
                    player = controller
                    connectionTimeoutJob?.cancel()
                    _connectionError.value = false
                    // 连接成功，重置退避延迟
                    currentRetryDelayMs = 1_000L
                    pendingUri?.let { uri ->
                        pendingUri = null
                        attachMediaItem(controller, uri)
                    }
                } catch (_: Throwable) {
                    _connectionError.value = true
                }
            },
            MoreExecutors.directExecutor()
        )

        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewModelScope.launch {
            delay(5000)
            if (!future.isDone && !future.isCancelled && player == null) {
                _connectionError.value = true
                future.cancel(true)
            }
        }
    }

    fun retryConnection() {
        val uri = currentUri ?: return  // Nothing to retry if no video loaded
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        player = null
        pendingUri = uri
        // 应用退避延迟后再连接
        val delay = currentRetryDelayMs
        currentRetryDelayMs = (currentRetryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
        viewModelScope.launch {
            delay(delay)
            connectController()
        }
    }

    fun clearConnectionError() {
        _connectionError.value = false
    }

    fun initializePlayer(uri: Uri) {
        if (initialized) return
        initialized = true
        currentUri = uri
        videoTitle = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?: "Video"

        initJob?.cancel()
        initJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val (effectiveW, effectiveH) = MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(getApplication<Application>(), uri)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    val ew = if (rot == 90 || rot == 270) h else w
                    val eh = if (rot == 90 || rot == 270) w else h
                    ew to eh
                }
                if (effectiveW > 0 && effectiveH > 0) {
                    withContext(Dispatchers.Main) {
                        videoWidth = effectiveW
                        videoHeight = effectiveH
                        isLandscapeVideo = effectiveW >= effectiveH
                        videoSizeKnown = true
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }

        val controller = player
        if (controller != null) {
            attachMediaItem(controller, uri)
        } else {
            pendingUri = uri
        }
    }

    private fun attachMediaItem(p: Player, uri: Uri) {
        viewModelScope.launch {
            val mediaItem = withContext(Dispatchers.IO) {
                SubtitleResolver.buildMediaItem(getApplication(), uri)
            }
            p.setMediaItem(mediaItem)
            p.prepare()
            p.playWhenReady = true
        }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
        showControls()
    }

    fun seekTo(positionMs: Long) {
        val p = player ?: return
        p.seekTo(positionMs.coerceIn(0L, duration))
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

    fun cycleSpeed() {
        val p = player ?: return
        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = speeds.indexOfFirst { it == playbackSpeed }.coerceAtLeast(0)
        playbackSpeed = speeds[(idx + 1) % speeds.size]
        p.setPlaybackSpeed(playbackSpeed)
    }

    fun updateVolume(percent: Int) { volumePercent = percent }

    fun updateBrightness(percent: Int) { brightnessPercent = percent }

    fun onPause() {
        savePosition()
    }

    fun onResume() {
        player?.play()
    }

    private fun savePosition() {
        val uri = currentUri ?: return
        val pos = player?.currentPosition ?: return
        viewModelScope.launch(Dispatchers.IO) {
            positionDao.upsert(PlaybackPositionEntity(uri.toString(), pos))
        }
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        if (player?.isPlaying == true) {
            hideControlsJob = viewModelScope.launch {
                delay(3000)
                controlsVisible = false
            }
        }
    }

    override fun onCleared() {
        val uri = currentUri
        val pos = player?.currentPosition ?: 0L
        if (uri != null) {
            viewModelScope.launch {
                positionDao.upsert(PlaybackPositionEntity(uri.toString(), pos))
            }
        }
        connectionTimeoutJob?.cancel()
        player?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        player = null
        super.onCleared()
    }
}