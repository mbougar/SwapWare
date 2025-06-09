package com.mbougar.swapware.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.repository.AuthRepository
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
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AuthViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val mockFirebaseUser: FirebaseUser = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with valid credentials updates state to success`() = runTest {
        val email = "test@test.com"
        val pass = "123456"
        coEvery { authRepository.login(email, pass) } returns Result.success(mockFirebaseUser)

        viewModel.authState.test {
            skipItems(1)

            viewModel.login(email, pass)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.isLoginSuccess).isTrue()
            assertThat(successState.currentUser).isEqualTo(mockFirebaseUser)
            assertThat(successState.error).isNull()
        }
    }

    @Test
    fun `login with invalid credentials updates state with error`() = runTest {
        val email = "test@test.com"
        val pass = "wrongpass"
        val error = "Invalid credentials"
        coEvery { authRepository.login(email, pass) } returns Result.failure(Exception(error))

        viewModel.login(email, pass)

        viewModel.authState.test {
            skipItems(1)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.isLoginSuccess).isFalse()
            assertThat(errorState.error).isEqualTo(error)
        }
    }

    @Test
    fun `login with blank email sets error and does not call repository`() = runTest {
        viewModel.login("", "password")

        viewModel.authState.test {
            val errorState = awaitItem()
            assertThat(errorState.error).isEqualTo("Email and password cannot be empty.")
        }
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `signup with valid data updates state to success`() = runTest {
        val email = "new@test.com"
        val pass = "123456"
        val displayName = "New User"
        coEvery { authRepository.signup(email, pass, displayName) } returns Result.success(mockFirebaseUser)

        viewModel.authState.test {
            skipItems(1)

            viewModel.signup(email, pass, displayName)

            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            val successState = awaitItem()
            assertThat(successState.isLoading).isFalse()
            assertThat(successState.isLoginSuccess).isTrue()
            assertThat(successState.currentUser).isEqualTo(mockFirebaseUser)
            assertThat(successState.error).isNull()
        }
    }

    @Test
    fun `signup with blank display name sets error`() = runTest {
        viewModel.signup("email@test.com", "password", "")

        viewModel.authState.test {
            val errorState = awaitItem()
            assertThat(errorState.error).isEqualTo("All fields must be filled.")
        }
        coVerify(exactly = 0) { authRepository.signup(any(), any(), any()) }
    }

    @Test
    fun `logout calls repository and resets state`() = runTest {
        viewModel.logout()

        coVerify { authRepository.logout() }
        viewModel.authState.test {
            val finalState = awaitItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.isLoginSuccess).isFalse()
            assertThat(finalState.currentUser).isNull()
            assertThat(finalState.error).isNull()
        }
    }

    @Test
    fun `consumeLoginSuccess resets the isLoginSuccess flag`() = runTest {
        val email = "test@test.com"
        val pass = "123456"
        coEvery { authRepository.login(email, pass) } returns Result.success(mockFirebaseUser)

        viewModel.login(email, pass)

        viewModel.authState.test {
            skipItems(2)
            val successState = awaitItem()
            assertThat(successState.isLoginSuccess).isTrue()

            viewModel.consumeLoginSuccess()
            val consumedState = awaitItem()
            assertThat(consumedState.isLoginSuccess).isFalse()
        }
    }
}
