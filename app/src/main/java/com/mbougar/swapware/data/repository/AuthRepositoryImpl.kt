package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.remote.FirebaseAuthSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthSource: FirebaseAuthSource
) : AuthRepository {
    override fun getCurrentUser() = firebaseAuthSource.getCurrentUser()
    override fun isUserLoggedIn() = firebaseAuthSource.isUserLoggedIn()
    override suspend fun login(email: String, pass: String) = firebaseAuthSource.login(email, pass)
    override suspend fun signup(email: String, pass: String, displayName: String) = firebaseAuthSource.signup(email, pass, displayName)
    override fun logout() = firebaseAuthSource.logout()
    override suspend fun updateUserProfilePicture(photoUrl: String): Result<Unit> {
        return firebaseAuthSource.updateProfilePicture(photoUrl)
    }
}