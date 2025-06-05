package com.mbougar.swapware.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class FirebaseAuthSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean = getCurrentUser() != null

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName
                }
                user.updateProfile(profileUpdates).await()
                Result.success(user)
            } else {
                Result.failure(Exception("User creation failed, user is null."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun updateProfilePicture(photoUrl: String): Result<Unit> { // New
        return try {
            val user = getCurrentUser()
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setPhotoUri(photoUrl.toUri())
                    .build()
                user.updateProfile(profileUpdates).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not logged in to update profile picture."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}