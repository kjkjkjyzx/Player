package com.example.player.data.subtitle

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

/**
 * 解析"同目录外挂字幕"。
 *
 * 场景：用户把 `<video>.mp4` 和 `<video>.srt` / `.ass` 放在同一目录下，
 * 播放视频时自动挂载字幕，无需手动选择。
 *
 * 做法：
 *  1. 通过 ContentResolver 查询视频 URI 的 `RELATIVE_PATH` + `DISPLAY_NAME`；
 *  2. 在 `MediaStore.Files` 里按同 `RELATIVE_PATH` 且文件名前缀匹配（不含扩展名）
 *     过滤，找到 `.srt` / `.ass` / `.ssa` / `.vtt` 扩展名的文件；
 *  3. 返回这些文件的 content URI 以及 Media3 的 Mime 类型。
 */
object SubtitleResolver {

    private data class SubtitleEntry(val uri: Uri, val mime: String, val language: String?)

    private val SUBTITLE_EXT_TO_MIME = mapOf(
        "srt" to MimeTypes.APPLICATION_SUBRIP,
        "vtt" to MimeTypes.TEXT_VTT,
        "ass" to MimeTypes.TEXT_SSA,
        "ssa" to MimeTypes.TEXT_SSA
    )

    /**
     * 给定视频 URI，构造带外挂字幕的 MediaItem。
     * 找不到字幕时返回 `MediaItem.fromUri(videoUri)`。
     */
    fun buildMediaItem(context: Context, videoUri: Uri): MediaItem {
        val entries = resolve(context, videoUri)
        if (entries.isEmpty()) return MediaItem.fromUri(videoUri)

        val subtitleConfigs = entries.mapIndexed { index, entry ->
            MediaItem.SubtitleConfiguration.Builder(entry.uri)
                .setMimeType(entry.mime)
                .setLanguage(entry.language ?: "und")
                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }

        return MediaItem.Builder()
            .setUri(videoUri)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()
    }

    // ── 私有实现 ─────────────────────────────────────────────────────────────

    private fun resolve(context: Context, videoUri: Uri): List<SubtitleEntry> {
        // 仅 MediaStore 视频 URI 可用 RELATIVE_PATH 方案。
        if (isMediaStoreVideoUri(videoUri)) {
            return resolveFromMediaStore(context, videoUri)
        }

        // 对其它 content URI（例如 SAF 导入）尝试走 DocumentFile 同目录扫描。
        return resolveFromSafSiblings(context, videoUri)
    }

    private fun resolveFromMediaStore(context: Context, videoUri: Uri): List<SubtitleEntry> {
        val (relPath, displayName) = queryVideoLocation(context, videoUri) ?: return emptyList()
        val baseName = displayName.substringBeforeLast('.').trim()
        if (baseName.isBlank()) return emptyList()

        val filesUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        // 在同目录（RELATIVE_PATH）里找以 baseName 开头、扩展名匹配的文件
        val selection =
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(relPath, "$baseName.%")

        val results = mutableListOf<SubtitleEntry>()
        runCatching {
            context.contentResolver.query(filesUri, projection, selection, args, null)?.use { cursor ->
                val idIdx   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: continue
                    val ext  = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
                    val mime = SUBTITLE_EXT_TO_MIME[ext] ?: continue
                    val id   = cursor.getLong(idIdx)
                    val uri  = ContentUris.withAppendedId(filesUri, id)
                    // 约定：`<base>.zh.srt` / `<base>.en.srt` 的第二段视为语言码
                    val middle = name.removePrefix("$baseName.")
                        .removeSuffix(".$ext")
                        .takeIf { it.isNotBlank() && it.length in 2..5 }
                    results += SubtitleEntry(uri, mime, middle)
                }
            }
        }
        // 默认语言（无中间段）放在第一位，让 setSelectionFlags(DEFAULT) 命中它
        return results.sortedBy { it.language != null }
    }

    private fun resolveFromSafSiblings(context: Context, videoUri: Uri): List<SubtitleEntry> {
        val displayName = queryDisplayName(context, videoUri) ?: return emptyList()
        val baseName = displayName.substringBeforeLast('.').trim()
        if (baseName.isBlank()) return emptyList()

        val current = DocumentFile.fromSingleUri(context, videoUri) ?: return emptyList()
        val parent = current.parentFile ?: return emptyList()
        val children = runCatching { parent.listFiles().toList() }.getOrDefault(emptyList())
        if (children.isEmpty()) return emptyList()

        val results = mutableListOf<SubtitleEntry>()
        for (child in children) {
            val name = child.name ?: continue
            val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            val mime = SUBTITLE_EXT_TO_MIME[ext] ?: continue
            if (!name.startsWith("$baseName.")) continue
            val middle = name.removePrefix("$baseName.")
                .removeSuffix(".$ext")
                .takeIf { it.isNotBlank() && it.length in 2..5 }
            results += SubtitleEntry(child.uri, mime, middle)
        }
        return results.sortedBy { it.language != null }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun isMediaStoreVideoUri(uri: Uri): Boolean {
        return uri.scheme == "content" &&
            uri.authority == MediaStore.AUTHORITY &&
            uri.pathSegments.firstOrNull() == "external"
    }

    /**
     * 返回视频的 `RELATIVE_PATH` 与 `DISPLAY_NAME`。
     * 仅当视频是 MediaStore 里的条目时才有效。
     */
    private fun queryVideoLocation(context: Context, videoUri: Uri): Pair<String, String>? {
        val projection = arrayOf(
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DISPLAY_NAME
        )
        return runCatching {
            context.contentResolver.query(videoUri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val relIdx  = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                val nameIdx = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val rel     = if (relIdx  >= 0) cursor.getString(relIdx)  else null
                val name    = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                if (rel.isNullOrBlank() || name.isNullOrBlank()) null
                else rel to name
            }
        }.getOrNull()
    }
}
