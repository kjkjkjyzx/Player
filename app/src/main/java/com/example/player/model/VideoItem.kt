package com.example.player.model

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val displayName: String,
    val duration: Long,
    val size: Long
) {
    val durationFormatted: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() = when {
            size >= 1_073_741_824 -> "%.1f GB".format(size / 1_073_741_824.0)
            size >= 1_048_576 -> "%.1f MB".format(size / 1_048_576.0)
            else -> "%.1f KB".format(size / 1024.0)
        }
}
