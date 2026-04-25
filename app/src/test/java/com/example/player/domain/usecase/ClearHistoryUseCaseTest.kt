package com.example.player.domain.usecase

import com.example.player.data.db.dao.PlaybackPositionDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ClearHistoryUseCaseTest {

    private lateinit var playbackPositionDao: PlaybackPositionDao
    private lateinit var useCase: ClearHistoryUseCase

    @Before
    fun setup() {
        playbackPositionDao = mockk(relaxed = true)
        useCase = ClearHistoryUseCase(playbackPositionDao)
    }

    @Test
    fun `invoke calls dao clear`() = runTest {
        useCase()
        coVerify { playbackPositionDao.clear() }
    }
}
