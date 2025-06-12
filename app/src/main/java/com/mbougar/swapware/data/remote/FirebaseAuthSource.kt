package com.mbougar.swapware.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * Gestiona todas las operaciones de autenticación con Firebase.
 * Es como el portero de la aplicación: controla quién entra, quién se registra y quién sale.
 *
 * @param firebaseAuth La instancia de FirebaseAuth que nos da Hilt.
 */
@Singleton
class FirebaseAuthSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Nos da el objeto del usuario que está actualmente logueado.
     * Si no hay nadie, devuelve null.
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Una forma rápida de saber si hay un usuario logueado o no.
     * @return `true` si hay alguien, `false` si no.
     */
    fun isUserLoggedIn(): Boolean = getCurrentUser() != null

    /**
     * Intenta iniciar sesión con un email y contraseña.
     *
     * @param email El correo del usuario.
     * @param pass La contraseña.
     * @return Un `Result` con el usuario si el login es correcto, o un error si no lo es.
     */
    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra un usuario nuevo en Firebase.
     *
     * @param email El correo para la nueva cuenta.
     * @param pass La contraseña para la nueva cuenta.
     * @param displayName El nombre público que tendrá el usuario.
     * @return Un `Result` con el usuario recién creado si todo va bien, o un error si falla.
     */
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

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Actualiza la foto de perfil del usuario en Firebase Auth.
     *
     * @param photoUrl La nueva URL de la foto de perfil.
     * @return Un `Result` que nos dice si la operación ha tenido éxito.
     */
    suspend fun updateProfilePicture(photoUrl: String): Result<Unit> {
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