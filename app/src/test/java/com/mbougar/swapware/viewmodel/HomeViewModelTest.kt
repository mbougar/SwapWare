package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.utils.LocationUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val poblacionDao: PoblacionDao = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private val adsFlow = MutableStateFlow<Result<List<Ad>>>(Result.success(emptyList()))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { adRepository.getAds() } returns adsFlow
        viewModel = HomeViewModel(adRepository, poblacionDao, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading and then updates with ads`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()

            val emptyState = awaitItem()
            assertThat(emptyState.isLoading).isFalse()
            assertThat(emptyState.ads).isEmpty()

            val fakeAds = listOf(Ad(id = "1", title = "GPU"))
            adsFlow.value = Result.success(fakeAds)

            val loadedState = awaitItem()
            assertThat(loadedState.ads).isEqualTo(fakeAds)
        }
    }

    @Test
    fun `when ad loading fails error state is updated`() = runTest {
        val error = Exception("Network error")

        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            adsFlow.value = Result.failure(error)

            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.error).isEqualTo(error.message)
        }
    }

    @Test
    fun `filterByCategory updates selected category and filters ads`() = runTest {
        val ads = listOf(
            Ad(id = "1", category = "CPU"),
            Ad(id = "2", category = "GPU")
        )

        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            adsFlow.value = Result.success(ads)

            assertThat(awaitItem().ads).hasSize(2)

            viewModel.filterByCategory("CPU")

            val filteredState = awaitItem()
            assertThat(filteredState.selectedCategory).isEqualTo("CPU")
            assertThat(filteredState.ads).hasSize(1)
            assertThat(filteredState.ads.first().id).isEqualTo("1")
        }
    }

    @Test
    fun `setting distance filter correctly filters ads by location`() = runTest {
        mockkObject(LocationUtils)
        val userLocation = PoblacionLocation("User Town", "Province", 40.0, -3.0)
        val closeAd = Ad(id = "1", title = "Close Ad", sellerLatitude = 40.01, sellerLongitude = -3.0)
        val farAd = Ad(id = "2", title = "Far Ad", sellerLatitude = 41.0, sellerLongitude = -4.0)

        every { LocationUtils.calculateDistanceKm(40.0, -3.0, 40.01, -3.0) } returns 1.1
        every { LocationUtils.calculateDistanceKm(40.0, -3.0, 41.0, -4.0) } returns 120.0

        viewModel.uiState.test {
            awaitItem()
            awaitItem()

            adsFlow.value = Result.success(listOf(closeAd, farAd))
            assertThat(awaitItem().ads).hasSize(2)

            viewModel.setDistanceFilterLocation(userLocation)
            val locationSetState = awaitItem()
            assertThat(locationSetState.userPoblacionForFilter).isEqualTo(userLocation)

            viewModel.setFilterDistanceKm(10f)
            val distanceSetState = awaitItem()
            assertThat(distanceSetState.filterDistanceKm).isEqualTo(10f)
            assertThat(distanceSetState.ads).hasSize(1)
            assertThat(distanceSetState.ads.first().id).isEqualTo("1")
        }
    }

    @Test
    fun `onFilterLocationSearchQueryChanged fetches suggestions after debounce`() = runTest {
        val query = "Madrid"
        val suggestions = listOf(PoblacionLocation("Madrid", "Madrid", 0.0, 0.0))
        coEvery { poblacionDao.searchPoblaciones(query, limit = 10) } returns suggestions

        viewModel.uiState.test {
            awaitItem()

            viewModel.onFilterLocationSearchQueryChanged(query)
            assertThat(awaitItem().locationSearchQuery).isEqualTo(query)
        }
    }

    @Test
    fun `toggleFavorite calls repository`() = runTest {
        val adId = "ad_to_favorite"
        val ad = Ad(id = adId, title = "Test Ad")
        coEvery { adRepository.getAdById(adId) } returns ad

        viewModel.toggleFavorite(adId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { adRepository.toggleFavorite(adId, true) }
    }
}
