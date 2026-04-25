package com.example.player.util

/**
 * 格式化播放进度时间。
 *
 * - 不足 1 小时：`mm:ss`
 * - 1 小时及以上：`h:mm:ss`
 */
fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
