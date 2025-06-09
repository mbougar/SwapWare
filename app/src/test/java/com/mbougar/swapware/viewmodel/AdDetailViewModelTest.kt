package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class AdDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AdDetailViewModel

    private val adRepository: AdRepository = mockk(relaxed = true)
    private val messageRepository: MessageRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("adId" to "1"))

    private val currentUserId = "user123"
    private val mockUser: FirebaseUser = mockk {
        every { uid } returns currentUserId
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUser() } returns mockUser
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = AdDetailViewModel(adRepository, messageRepository, authRepository, savedStateHandle)
    }

    @Test
    fun `loadAdDetails success updates uiState with ad`() = runTest {
        val ad = Ad(id = "1", title = "Test Ad")
        coEvery { adRepository.getAdById("1") } returns ad

        createViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.ad).isEqualTo(ad)
            assertThat(successState.isOwnAd).isFalse()
        }
    }

    @Test
    fun `loadAdDetails for own ad sets isOwnAd to true`() = runTest {
        val ad = Ad(id = "1", title = "Test Ad", sellerId = currentUserId)
        coEvery { adRepository.getAdById("1") } returns ad

        createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertThat(state.isOwnAd).isTrue()
        }
    }

    @Test
    fun `initiateConversation success navigates with conversationId`() = runTest {
        val ad = Ad(id = "1", title = "Test Ad", sellerId = "seller456")
        coEvery { adRepository.getAdById("1") } returns ad
        coEvery { messageRepository.findOrCreateConversationForAd(ad) } returns Result.success("conv1")

        createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val loadedState = awaitItem()
            assertThat(loadedState.ad).isEqualTo(ad)

            viewModel.initiateConversation()

            val initiatingState = awaitItem()
            assertThat(initiatingState.isInitiatingConversation).isTrue()

            val successState = awaitItem()
            assertThat(successState.isInitiatingConversation).isFalse()
            assertThat(successState.navigateToConversationId).isEqualTo("conv1")
        }
    }

    @Test
    fun `initiateConversation failure sets conversationError`() = runTest {
        val ad = Ad(id = "1", title = "Test Ad", sellerId = "seller456")
        val error = "Failed to create"
        coEvery { adRepository.getAdById("1") } returns ad
        coEvery { messageRepository.findOrCreateConversationForAd(ad) } returns Result.failure(Exception(error))

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val loadedState = awaitItem()
            assertThat(loadedState.ad).isEqualTo(ad)

            viewModel.initiateConversation()

            val initiatingState = awaitItem()
            assertThat(initiatingState.isInitiatingConversation).isTrue()

            val errorState = awaitItem()
            assertThat(errorState.isInitiatingConversation).isFalse()
            assertThat(errorState.conversationError).isEqualTo(error)
        }
    }
}
