package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FavoritesViewModel
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadFavoriteAds success updates state`() = runTest {
        val favAds = listOf(Ad(id = "fav1", isFavorite = true))
        coEvery { adRepository.getFavoriteAds() } returns flowOf(favAds)

        viewModel = FavoritesViewModel(adRepository, authRepository)

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.favoriteAds).isEqualTo(favAds)
        }
    }

    @Test
    fun `removeFromFavorites calls repository with correct parameters`() = runTest {
        coEvery { adRepository.getFavoriteAds() } returns flowOf(emptyList())
        viewModel = FavoritesViewModel(adRepository, authRepository)

        val adIdToRemove = "fav1"
        viewModel.removeFromFavorites(adIdToRemove)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { adRepository.toggleFavorite(adIdToRemove, false) }
    }
}

