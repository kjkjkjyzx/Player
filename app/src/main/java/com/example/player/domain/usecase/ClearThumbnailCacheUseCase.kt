package com.example.player.domain.usecase

import android.content.Context
import com.example.player.data.thumbs.ThumbnailCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ClearThumbnailCacheUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean {
        return runCatching {
            ThumbnailCache.dir(context).deleteRecursively()
        }.isSuccess
    }
}
