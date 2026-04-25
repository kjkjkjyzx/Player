package com.example.player.domain.usecase

import android.content.Context
import com.example.player.data.thumbs.ThumbnailCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test

class GetThumbnailCacheSizeUseCaseTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk()
        mockkObject(ThumbnailCache)
    }

    @After
    fun teardown() {
        unmockkObject(ThumbnailCache)
    }

    @Test
    fun `invoke returns size from ThumbnailCache`() {
        every { ThumbnailCache.currentSizeBytes(mockContext) } returns 1_234_567L
        val useCase = GetThumbnailCacheSizeUseCase(mockContext)
        org.junit.Assert.assertEquals(1_234_567L, useCase())
    }
}
