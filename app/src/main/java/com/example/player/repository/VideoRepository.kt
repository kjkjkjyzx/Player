package com.example.player.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.player.R
import com.example.player.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 视频数据仓库契约。
 *
 * 目的：把 MediaStore 查询与元数据解析从 ViewModel 彻底解耦；
 * `observeLocalVideos()` 订阅 MediaStore ContentObserver，
 * 库变动（新拷入、删除）自动推新列表，免去手动刷新。
 */
interface VideoRepository {
    /** 一次性查询 MediaStore 内所有本地视频。 */
    suspend fun getLocalVideos(): List<VideoItem>

    /** 订阅 MediaStore 变更；emit 当前列表，之后每次 ContentObserver 通知都再发一次。 */
    fun observeLocalVideos(): Flow<List<VideoItem>>

    /** 解析任意 content URI（例如文件选择器返回）的视频元数据。 */
    suspend fun getVideoMetadata(uri: Uri): VideoItem
}

/**
 * 基于 Android MediaStore 的默认实现。
 */
class MediaStoreVideoRepository(private val context: Context) : VideoRepository {

    override suspend fun getLocalVideos(): List<VideoItem> = queryLocalVideos()

    override fun observeLocalVideos(): Flow<List<VideoItem>> = channelFlow {
        // 初始发射
        launch { trySend(queryLocalVideos()) }

        val handler  = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                // 每次 MediaStore 通知都重新查询，channelFlow 会并发地把结果送到下游；
                // conflate() 保证只保留最新一帧，避免连续抖动导致的背压。
                launch { trySend(queryLocalVideos()) }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants = */ true,
            observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)

    override suspend fun getVideoMetadata(uri: Uri): VideoItem {
        var name = uri.lastPathSegment ?: context.getString(R.string.unknown_video)
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        }
        var duration    = 0L
        var isLandscape = false
        // use { } 保证异常路径下 native 资源也会释放（MediaMetadataRetriever 自 API 29 起实现 AutoCloseable，minSdk=31 满足）
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val w   = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()    ?: 0
                val h   = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()   ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val effectiveW = if (rot == 90 || rot == 270) h else w
                val effectiveH = if (rot == 90 || rot == 270) w else h
                if (effectiveW > 0 && effectiveH > 0) isLandscape = effectiveW >= effectiveH
            }
        }
        return VideoItem(uri, name, duration, size, isLandscape)
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun queryLocalVideos(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn       = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn    = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn   = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idColumn)
                val name     = cursor.getString(nameColumn)
                val duration = cursor.getLong(durationColumn)
                val size     = cursor.getLong(sizeColumn)
                val w        = if (widthColumn  >= 0) cursor.getInt(widthColumn)  else 0
                val h        = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
                val isLandscape = w > 0 && h > 0 && w >= h
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                videos.add(VideoItem(contentUri, name, duration, size, isLandscape))
            }
        }
        return videos
    }
}
