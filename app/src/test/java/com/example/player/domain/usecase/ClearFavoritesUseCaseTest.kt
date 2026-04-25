package com.example.player.domain.usecase

import com.example.player.data.db.dao.FavoriteDao
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ClearFavoritesUseCaseTest {

    private lateinit var favoriteDao: FavoriteDao
    private lateinit var useCase: ClearFavoritesUseCase

    @Before
    fun setup() {
        favoriteDao = mockk(relaxed = true)
        useCase = ClearFavoritesUseCase(favoriteDao)
    }

    @Test
    fun `invoke calls dao clear`() = runTest {
        useCase()
        coVerify { favoriteDao.clear() }
    }
}
