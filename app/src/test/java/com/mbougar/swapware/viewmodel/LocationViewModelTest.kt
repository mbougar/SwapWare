package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.local.PoblacionLocation
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
class LocationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LocationViewModel
    private val poblacionDao: PoblacionDao = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LocationViewModel(poblacionDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSearchQueryChanged with short query does not trigger search`() = runTest {
        viewModel.onSearchQueryChanged("a")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { poblacionDao.searchPoblaciones(any()) }
        assertThat(viewModel.searchResults.value).isEmpty()
    }

    @Test
    fun `onSearchQueryChanged with valid query triggers search and updates results`() = runTest {
        val query = "Madrid"
        val mockResults = listOf(PoblacionLocation("Madrid", "Madrid", 0.0, 0.0))
        coEvery { poblacionDao.searchPoblaciones("$query%") } returns mockResults

        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.searchResults.value).isEqualTo(mockResults)
    }

    @Test
    fun `onSearchQueryChanged with blank query clears search results`() = runTest {
        val query = "Barcelona"
        val mockResults = listOf(PoblacionLocation("Barcelona", "Barcelona", 0.0, 0.0))
        coEvery { poblacionDao.searchPoblaciones("$query%") } returns mockResults
        viewModel.onSearchQueryChanged(query)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.searchResults.value).isNotEmpty()

        viewModel.onSearchQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.searchResults.value).isEmpty()
    }

    @Test
    fun `onPoblacionSelected updates selected poblacion and clears search results`() = runTest {
        val selectedPoblacion = PoblacionLocation("Valencia", "Valencia", 1.0, 1.0)

        viewModel.searchResults.test {
            assertThat(awaitItem()).isEmpty()

            viewModel.onSearchQueryChanged("val")
            val mockResults = listOf(selectedPoblacion)
            coEvery { poblacionDao.searchPoblaciones("val%") } returns mockResults
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(mockResults)

            viewModel.onPoblacionSelected(selectedPoblacion)

            assertThat(awaitItem()).isEmpty()
        }

        assertThat(viewModel.selectedPoblacion.value).isEqualTo(selectedPoblacion)
        assertThat(viewModel.searchQuery.value).isEqualTo(selectedPoblacion.getDisplayName())
    }

    @Test
    fun `clearSelection resets all states`() = runTest {
        val poblacion = PoblacionLocation("Sevilla", "Sevilla", 2.0, 2.0)
        viewModel.onSearchQueryChanged("Sev")
        coEvery { poblacionDao.searchPoblaciones("Sev") } returns listOf(poblacion)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onPoblacionSelected(poblacion)

        assertThat(viewModel.selectedPoblacion.value).isNotNull()
        assertThat(viewModel.searchQuery.value).isNotEmpty()

        viewModel.clearSelection()

        assertThat(viewModel.selectedPoblacion.value).isNull()
        assertThat(viewModel.searchQuery.value).isEmpty()
        assertThat(viewModel.searchResults.value).isEmpty()
    }
}
