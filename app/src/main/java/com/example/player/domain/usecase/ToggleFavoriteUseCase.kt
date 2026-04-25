package com.example.player.domain.usecase

import android.net.Uri
import com.example.player.data.db.dao.FavoriteDao
import com.example.player.data.db.entity.FavoriteEntity
import javax.inject.Inject

/**
 * 切换视频收藏状态。
 * 若已收藏则取消，否则添加。
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    suspend operator fun invoke(uri: Uri) {
        val key = uri.toString()
        if (favoriteDao.isFavorite(key)) {
            favoriteDao.remove(key)
        } else {
            favoriteDao.add(FavoriteEntity(key))
        }
    }
}
