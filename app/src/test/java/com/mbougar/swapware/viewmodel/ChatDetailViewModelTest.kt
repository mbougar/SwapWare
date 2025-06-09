package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.model.UserRating
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URLDecoder

@ExperimentalCoroutinesApi
class ChatDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChatDetailViewModel

    private val messageRepository: MessageRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private lateinit var savedStateHandle: SavedStateHandle

    private val currentUserId = "user1"
    private val otherUserId = "user2"
    private val adId = "ad1"
    private val conversationId = "conv1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockUser: FirebaseUser = mockk { every { uid } returns currentUserId }
        coEvery { authRepository.getCurrentUser() } returns mockUser
        savedStateHandle = SavedStateHandle(mapOf(
            "conversationId" to conversationId,
            "otherUserDisplayName" to "Other User",
            "adTitle" to "Test Ad"
        ))
        mockkStatic(URLDecoder::class)
        every { URLDecoder.decode(any(), "UTF-8") } answers { firstArg() }

        val defaultAd = Ad(id = adId)
        val defaultConv = Conversation(id = conversationId, adId = adId)
        coEvery { messageRepository.getConversationDetails(conversationId) } returns Result.success(defaultConv)
        coEvery { messageRepository.getAdDetailsForConversation(adId) } returns defaultAd
        coEvery { messageRepository.getMessagesStream(conversationId) } returns flowOf(Result.success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(URLDecoder::class)
    }

    @Test
    fun `init loads conversation, ad, and messages successfully`() = runTest {
        viewModel = ChatDetailViewModel(messageRepository, authRepository, savedStateHandle)
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isTrue()
            assertThat(finalState.error).isNull()
        }
    }

    @Test
    fun `sendMessage with blank text does not call repository`() = runTest {
        viewModel = ChatDetailViewModel(messageRepository, authRepository, savedStateHandle)

        viewModel.onInputTextChanged("   ")
        viewModel.sendMessage()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { messageRepository.sendMessage(any(), any()) }
    }

    @Test
    fun `markAdAsSoldToOtherUser success updates state`() = runTest {
        val initialAd = Ad(id = adId, sellerId = currentUserId, sold = false)
        val initialConv = Conversation(id = conversationId, adId = adId, participantIds = listOf(currentUserId, otherUserId))

        coEvery { messageRepository.getConversationDetails(conversationId) } returns Result.success(initialConv)
        coEvery { messageRepository.getAdDetailsForConversation(adId) } returns initialAd

        viewModel = ChatDetailViewModel(messageRepository, authRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedAd = initialAd.copy(sold = true)
        val updatedConv = initialConv.copy(adIsSoldInThisConversation = true)
        coEvery { messageRepository.markAdAsSoldViaConversation(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { messageRepository.getAdDetailsForConversation(adId) } returns updatedAd
        coEvery { messageRepository.getConversationDetails(conversationId) } returns Result.success(updatedConv)

        viewModel.uiState.test {
            awaitItem()

            viewModel.markAdAsSoldToOtherUser()

            assertThat(awaitItem().isLoading).isTrue()
            val finalState = awaitItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.adDetails?.sold).isTrue()
            assertThat(finalState.conversationDetails?.adIsSoldInThisConversation).isTrue()
        }
    }

    @Test
    fun `submitRating by buyer calls repository with isSellerSubmitting as false`() = runTest {
        val sellerId = "user2_seller"
        val buyerId = currentUserId
        val ad = Ad(id = adId, sellerId = sellerId)
        val conversation = Conversation(id = conversationId, adId = adId, participantIds = listOf(buyerId, sellerId), adSoldToParticipantId = buyerId, sellerRatedBuyerForAd = false, buyerRatedSellerForAd = false)

        coEvery { messageRepository.getConversationDetails(conversationId) } returns Result.success(conversation)
        coEvery { messageRepository.getAdDetailsForConversation(adId) } returns ad
        coEvery { messageRepository.submitRatingAndUpdateProfile(any(), conversationId, false) } returns Result.success(Unit)

        viewModel = ChatDetailViewModel(messageRepository, authRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onAttemptToRateUser(sellerId)
            assertThat(awaitItem().showRatingDialogForUser).isEqualTo(sellerId)

            viewModel.submitRating(4)
            assertThat(awaitItem().ratingSubmissionInProgress).isTrue()

            awaitItem()
        }

        coVerify { messageRepository.submitRatingAndUpdateProfile(any<UserRating>(), conversationId, false) }
    }
}
