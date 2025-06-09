package com.mbougar.swapware.data.repository

import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk
import javax.inject.Inject

class FakeAuthRepository @Inject constructor() : AuthRepository {

    private var shouldLoginSucceed = true
    private var isUserLoggedIn = false

    fun setLoginSuccess(shouldSucceed: Boolean) {
        shouldLoginSucceed = shouldSucceed
    }

    override fun getCurrentUser(): FirebaseUser? {
        return if (isUserLoggedIn) mockk(relaxed = true) else null
    }

    override fun isUserLoggedIn(): Boolean {
        return isUserLoggedIn
    }

    override suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return if (shouldLoginSucceed) {
            isUserLoggedIn = true
            Result.success(mockk(relaxed = true))
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun signup(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return Result.success(mockk(relaxed = true))
    }

    override fun logout() {
        isUserLoggedIn = false
    }

    override suspend fun updateUserProfilePicture(photoUrl: String): Result<Unit> {
        return Result.success(Unit)
    }
}
