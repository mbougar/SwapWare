package com.mbougar.swapware.viewmodel

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
import com.mbougar.swapware.data.model.NewAdData
import com.mbougar.swapware.data.repository.AdRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AddAdViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddAdViewModel
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val poblacionDao: PoblacionDao = mockk(relaxed = true)
    private val mockUri: Uri = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AddAdViewModel(adRepository, poblacionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addAd with valid data calls repository and sets success state`() = runTest {
        val poblacion = PoblacionLocation("Test City", "Test Province", 1.0, 1.0)
        viewModel.onPoblacionSelected(poblacion)
        coEvery { adRepository.addAd(any()) } returns Result.success(Unit)

        viewModel.uiState.test {
            assertThat(awaitItem().isSuccess).isFalse()

            viewModel.addAd("Title", "Desc", "100", "CPU", mockUri)

            assertThat(awaitItem().isLoading).isTrue()
            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.isSuccess).isTrue()
        }

        coVerify { adRepository.addAd(any<NewAdData>()) }
    }

    @Test
    fun `addAd with blank title sets error and does not call repository`() = runTest {
        val poblacion = PoblacionLocation("Test City", "Test Province", 1.0, 1.0)
        viewModel.onPoblacionSelected(poblacion)

        viewModel.addAd("", "Desc", "100", "CPU", null)

        viewModel.uiState.test {
            val errorState = awaitItem()
            assertThat(errorState.error).contains("Please fill all fields")
        }
        coVerify(exactly = 0) { adRepository.addAd(any()) }
    }

    @Test
    fun `addAd with zero price sets error`() = runTest {
        val poblacion = PoblacionLocation("Test City", "Test Province", 1.0, 1.0)
        viewModel.onPoblacionSelected(poblacion)

        viewModel.addAd("Title", "Desc", "0", "CPU", null)

        viewModel.uiState.test {
            val errorState = awaitItem()
            assertThat(errorState.error).isEqualTo("Price must be positive.")
        }
    }

    @Test
    fun `onLocationSearchQueryChanged updates query and fetches suggestions`() = runTest {
        val query = "Valencia"
        val suggestions = listOf(PoblacionLocation(query, query, 0.0, 0.0))
        coEvery { poblacionDao.searchPoblaciones("$query%", limit = 10) } returns suggestions

        viewModel.onLocationSearchQueryChanged(query)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.locationSearchQuery.value).isEqualTo(query)
        assertThat(viewModel.locationSuggestions.value).isEqualTo(suggestions)
    }

    @Test
    fun `resetState clears all ui states and queries`() = runTest {
        viewModel.onLocationSearchQueryChanged("test")
        viewModel.addAd("t", "d", "1", "c", null)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isSuccess).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.selectedPoblacion).isNull()
        assertThat(viewModel.locationSearchQuery.value).isEmpty()
        assertThat(viewModel.locationSuggestions.value).isEmpty()
    }
}
