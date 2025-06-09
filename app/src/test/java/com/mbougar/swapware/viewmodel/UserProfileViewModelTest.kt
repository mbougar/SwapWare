package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.remote.FirestoreSource
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@ExperimentalCoroutinesApi
class UserProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UserProfileViewModel

    private val firestoreSource: FirestoreSource = mockk(relaxed = true)
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private lateinit var savedStateHandle: SavedStateHandle

    private val loggedInUserId = "user123"
    private val profileUserId = "user456"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockUser: FirebaseUser = mockk { every { uid } returns loggedInUserId }
        coEvery { authRepository.getCurrentUser() } returns mockUser
        viewModel = UserProfileViewModel(firestoreSource, adRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUserProfile success fetches profile and ads`() = runTest {
        val profileData = UserProfileData(displayName = "Profile User")
        val userAds = listOf(Ad(id = "ad1", sellerId = profileUserId))

        coEvery { firestoreSource.getUserProfile(profileUserId) } returns Result.success(profileData)
        coEvery { adRepository.getAdsByUserId(profileUserId) } returns flowOf(Result.success(userAds))

        viewModel = UserProfileViewModel(firestoreSource, adRepository, authRepository)
        viewModel.loadUserProfile(profileUserId)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.userDisplayName).isEqualTo("Profile User")
            assertThat(successState.userAds).isEqualTo(userAds)
            assertThat(successState.error).isNull()
        }
    }
}
