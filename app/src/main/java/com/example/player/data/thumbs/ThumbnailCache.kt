package com.example.player.data.thumbs

import android.content.Context
import android.content.SharedPreferences
import com.example.player.data.thumbs.ThumbnailCache.SizeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频缩略图磁盘缓存，带大小上限控制。
 *
 * 每个视频 URI 对应 `cacheDir/thumbs/<sha1(uri)>.jpg`，
 * Coil 在 UI 层优先读取此文件，无则回退到 VideoFrameDecoder（主线程解码风险）。
 *
 * 缓存上限：50MB，达到上限后按修改时间淘汰最旧的条目。
 */
object ThumbnailCache {

    private const val DIR_NAME   = "thumbs"
    /** 缓存上限：50 MB */
    const val MAX_BYTES: Long = 50L * 1024 * 1024

    /** 输出尺寸（列表卡片 96×72 dp 最多 3× 放大，256×144 足够清晰） */
    const val TARGET_WIDTH  = 256
    const val TARGET_HEIGHT = 144

    fun dir(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun fileFor(context: Context, uri: String): File =
        File(dir(context), sha1(uri) + ".jpg")

    /** 返回已存在且非空的缓存文件；无则 null */
    fun existing(context: Context, uri: String): File? =
        fileFor(context, uri).takeIf { it.exists() && it.length() > 0L }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { b -> "%02x".format(b) }
    }

    /**
     * 写入缓存后调用，更新计数器并在超出上限时淘汰旧条目。
     * 由 [SizeManager] 在后台线程调用。
     */
    fun evictIfNeeded(context: Context, newBytes: Long) {
        val sm = SizeManager(context)
        sm.ensureLimit(newBytes)
    }

    /**
     * 查询当前缓存总大小（字节）。
     * 由 [SizeManager] 在后台线程调用。
     */
    fun currentSizeBytes(context: Context): Long {
        return SizeManager(context).currentSize()
    }

    /**
     * 管理缓存大小计数，基于 SharedPreferences 持久化。
     * 计数在 [ThumbnailWorker] 写入后由 [ThumbnailCache.evictIfNeeded] 调用。
     */
    @Singleton
    class SizeManager @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences("thumbnail_cache_meta", Context.MODE_PRIVATE)

        private companion object {
            const val KEY_TOTAL_BYTES = "total_bytes"
        }

        /** 返回当前已记录的缓存大小（字节） */
        fun currentSize(): Long = prefs.getLong(KEY_TOTAL_BYTES, 0L)

        /**
         * 更新计数器并在超出 [MAX_BYTES] 时淘汰最旧文件。
         * @param addedBytes 刚写入的新文件大小
         */
        fun ensureLimit(addedBytes: Long) {
            val dir = dir(context)
            val currentTotal = currentSize()
            val newTotal = currentTotal + addedBytes

            if (newTotal <= MAX_BYTES) {
                prefs.edit().putLong(KEY_TOTAL_BYTES, newTotal).apply()
                return
            }

            // 需要淘汰：按修改时间升序（最旧的在前面）
            val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
            var size = currentTotal
            for (f in files) {
                if (size + addedBytes <= MAX_BYTES) break
                val len = f.length()
                f.delete()
                size -= len
            }
            prefs.edit().putLong(KEY_TOTAL_BYTES, size + addedBytes).apply()
        }

        /**
         * 重置计数器（清除缓存后调用）。
         */
        fun reset() {
            prefs.edit().putLong(KEY_TOTAL_BYTES, 0L).apply()
        }
    }
}
