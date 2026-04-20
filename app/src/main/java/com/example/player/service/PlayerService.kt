package com.example.player.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 MediaSessionService，集中持有 ExoPlayer 与 MediaSession。
 *
 * 设计要点：
 *  - UI 层（PlayerViewModel）通过 MediaController 远程连接，不再直接持有 ExoPlayer；
 *  - 锁屏控件 / 蓝牙耳机按键 / 通知区播控均由 Media3 默认 NotificationProvider 提供；
 *  - 任务被系统移除且当前不在播放时主动 stopSelf，避免残留前台通知；
 *  - 允许多个 Controller 连接（UI + 系统媒体按钮），因此不对 onGetSession 做过滤。
 */
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(
                DefaultRenderersFactory(this)
                    .setEnableDecoderFallback(true)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus= */ true
            )
            // 进入后台播放时仍保持 Wake Lock，避免熄屏中断
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 用户从最近任务中划掉应用：若此刻没有在放/不愿继续放，则干净收尾。
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
