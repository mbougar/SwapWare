package com.mbougar.swapware.data.repository

import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirestoreSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource
) : AuthRepository {
    override fun getCurrentUser() = firebaseAuthSource.getCurrentUser()
    override fun isUserLoggedIn() = firebaseAuthSource.isUserLoggedIn()
    override suspend fun login(email: String, pass: String) = firebaseAuthSource.login(email, pass)
    override suspend fun signup(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        val signupResult = firebaseAuthSource.signup(email, pass, displayName)
        if (signupResult.isSuccess) {
            signupResult.getOrNull()?.let { user ->
                firestoreSource.createUserProfileDocument(user.uid, user.displayName, user.email)
            }
        }
        return signupResult
    }
    override fun logout() = firebaseAuthSource.logout()
    override suspend fun updateUserProfilePicture(photoUrl: String): Result<Unit> {
        return firebaseAuthSource.updateProfilePicture(photoUrl)
    }
}