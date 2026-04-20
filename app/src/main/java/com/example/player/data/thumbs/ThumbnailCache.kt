package com.example.player.data.thumbs

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * 视频缩略图磁盘缓存约定。
 *
 * 每个视频 URI 对应 `cacheDir/thumbs/<sha1(uri)>.jpg`，
 * Coil 在 UI 层优先读取此文件，无则回退到 VideoFrameDecoder（主线程解码风险）。
 */
object ThumbnailCache {

    private const val DIR_NAME = "thumbs"

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
}
