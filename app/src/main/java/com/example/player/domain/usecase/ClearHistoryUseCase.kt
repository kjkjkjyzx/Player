package com.example.player.domain.usecase

import com.example.player.data.db.dao.PlaybackPositionDao
import javax.inject.Inject

class ClearHistoryUseCase @Inject constructor(
    private val playbackPositionDao: PlaybackPositionDao
) {
    suspend operator fun invoke() {
        playbackPositionDao.clear()
    }
}
