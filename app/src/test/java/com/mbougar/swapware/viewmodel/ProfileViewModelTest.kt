package com.mbougar.swapware.viewmodel

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.remote.FirebaseStorageSource
import com.mbougar.swapware.data.remote.FirestoreSource
import io.mockk.*
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfileViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val storageSource: FirebaseStorageSource = mockk(relaxed = true)
    private val firestoreSource: FirestoreSource = mockk(relaxed = true)

    private val userId = "user123"
    private val mockUser: FirebaseUser = mockk {
        every { uid } returns userId
        every { email } returns "test@test.com"
        every { displayName } returns "Test User"
        every { photoUrl } returns null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUser() } returns mockUser

        coEvery { firestoreSource.getUserProfile(userId) } returns Result.success(UserProfileData(userId = userId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout calls repository`() = runTest {
        viewModel = ProfileViewModel(authRepository, storageSource, firestoreSource)
        viewModel.logout()
        coVerify { authRepository.logout() }
    }
}
