package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MyAdsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MyAdsViewModel
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private val userId = "myUserId"
    private val mockUser: FirebaseUser = mockk { every { uid } returns userId }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUser() } returns mockUser
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when user not logged in, state is error`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns null
        viewModel = MyAdsViewModel(adRepository, authRepository)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo("User not logged in.")
    }

    @Test
    fun `loadMyAds success updates uiState with ads`() = runTest {
        val myAds = listOf(Ad(id = "1", sellerId = userId))
        coEvery { adRepository.getAdsByUserId(userId) } returns flowOf(Result.success(myAds))

        viewModel = MyAdsViewModel(adRepository, authRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.ads).isEqualTo(myAds)
        }
    }
}
