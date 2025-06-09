package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MessagesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MessagesViewModel
    private val messageRepository: MessageRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockUser: FirebaseUser = mockk { every { uid } returns "user123" }
        coEvery { authRepository.getCurrentUser() } returns mockUser
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadConversations success updates state with conversations`() = runTest {
        val conversations = listOf(Conversation(id = "conv1"))
        coEvery { messageRepository.getConversationsStream() } returns flowOf(Result.success(conversations))

        viewModel = MessagesViewModel(messageRepository, authRepository)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.conversations).isEqualTo(conversations)
            assertThat(successState.error).isNull()
        }
    }

    @Test
    fun `loadConversations failure updates state with error`() = runTest {
        val originalErrorMessage = "Network failed"
        coEvery { messageRepository.getConversationsStream() } returns flow {
            emit(Result.failure(Exception(originalErrorMessage)))
        }

        viewModel = MessagesViewModel(messageRepository, authRepository)

        viewModel.uiState.test {
            awaitItem()

            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.error).isEqualTo(originalErrorMessage)
        }
    }
}
