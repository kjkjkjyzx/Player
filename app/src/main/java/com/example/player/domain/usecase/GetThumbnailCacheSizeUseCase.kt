package com.example.player.domain.usecase

import android.content.Context
import com.example.player.data.thumbs.ThumbnailCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 返回当前缩略图缓存总大小（字节）。
 * UI 层可用于在"清除缓存"按钮旁显示"当前缓存：xx MB"。
 */
class GetThumbnailCacheSizeUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Long = ThumbnailCache.currentSizeBytes(context)
}
