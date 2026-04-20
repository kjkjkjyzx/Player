package com.example.player.domain.usecase

import com.example.player.data.db.dao.FavoriteDao
import javax.inject.Inject

class ClearFavoritesUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    suspend operator fun invoke() {
        favoriteDao.clear()
    }
}
