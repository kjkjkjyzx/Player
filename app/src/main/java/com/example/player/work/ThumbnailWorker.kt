package com.example.player.work

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.player.data.db.dao.ImportedVideoDao
import com.example.player.data.thumbs.ThumbnailCache
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream

/**
 * 后台批量生成视频缩略图。
 *
 * - 扫描 MediaStore 本地视频 + Room 里用户导入的视频；
 * - 每个视频尝试在 1s 处抽帧 → 缩放到 256×144 → 写入 cacheDir/thumbs/<sha1(uri)>.jpg；
 * - 已有缓存的跳过；失败静默（Coil 会自动回退 VideoFrameDecoder）；
 * - 单次最多处理 [MAX_PER_RUN] 个，防止长时间占用 IO 与 CPU。
 *
 * 使用 Hilt EntryPoint 注入依赖（而非 `hilt-work`），避免引入额外 KSP 处理器。
 */
class ThumbnailWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun importedVideoDao(): ImportedVideoDao
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(ctx, WorkerEntryPoint::class.java)

        val uris = buildList {
            addAll(queryMediaStoreUris(ctx))
            addAll(queryImportedUris(entryPoint))
        }.distinct()

        var processed = 0
        for (uri in uris) {
            if (processed >= MAX_PER_RUN) break
            if (isStopped) return@withContext Result.success()
            yield()

            val target = ThumbnailCache.fileFor(ctx, uri)
            if (target.exists() && target.length() > 0L) continue

            val ok = runCatching { extractTo(ctx, Uri.parse(uri), target) }.getOrDefault(false)
            if (ok) {
                processed++
                // 更新缓存大小计数器，超限时淘汰旧条目
                ThumbnailCache.evictIfNeeded(ctx, target.length())
            }
        }
        Result.success()
    }

    private fun queryMediaStoreUris(ctx: Context): List<String> {
        val out = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Video.Media._ID)
        ctx.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                out += uri.toString()
                if (out.size >= MAX_PER_RUN * 4) break   // 限制内存
            }
        }
        return out
    }

    private suspend fun queryImportedUris(ep: WorkerEntryPoint): List<String> {
        // ImportedVideoDao 只暴露 Flow，这里用 first() 拿最近快照
        return try {
            ep.importedVideoDao().observeAll().first().map { it.uri }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun extractTo(ctx: Context, uri: Uri, target: File): Boolean {
        val bitmap: Bitmap = MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(ctx, uri)
            // 1s 位置抽帧（微秒）
            retriever.getScaledFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                ThumbnailCache.TARGET_WIDTH,
                ThumbnailCache.TARGET_HEIGHT
            ) ?: retriever.getFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } ?: return false

        val tmp = File(target.parentFile, target.name + ".tmp")
        return try {
            FileOutputStream(tmp).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }
            bitmap.recycle()
            tmp.renameTo(target)
        } catch (_: Throwable) {
            tmp.delete()
            false
        }
    }

    companion object {
        const val UNIQUE_NAME = "thumbnail-prewarm"
        private const val MAX_PER_RUN = 120
        private const val JPEG_QUALITY = 82
    }
}
