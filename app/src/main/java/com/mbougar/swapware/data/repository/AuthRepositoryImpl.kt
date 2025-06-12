package com.mbougar.swapware.data.repository

import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirestoreSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de autenticación.
 * Es el que de verdad hace el trabajo, conectando la lógica de la app
 * con Firebase Auth y Firestore para todo lo relacionado con los usuarios.
 *
 * @param firebaseAuthSource La fuente de datos para la autenticación (login, registro).
 * @param firestoreSource La fuente de datos para la base de datos (guardar perfiles de usuario).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource
) : AuthRepository {
    /**
     * Obtiene de FirebaseAuthSource el usuario actual.
     */
    override fun getCurrentUser() = firebaseAuthSource.getCurrentUser()

    /**
     * Le pregunta a FirebaseAuthSource si hay alguien logueado.
     */
    override fun isUserLoggedIn() = firebaseAuthSource.isUserLoggedIn()

    /**
     * Llama a la fuente de datos para que inicie sesión. No hace nada más.
     */
    override suspend fun login(email: String, pass: String) = firebaseAuthSource.login(email, pass)

    /**
     * Orquesta el proceso de registro.
     * 1. Llama a FirebaseAuthSource para crear el usuario.
     * 2. Si se crea bien, llama a FirestoreSource para crear su documento de perfil.
     */
    override suspend fun signup(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        val signupResult = firebaseAuthSource.signup(email, pass, displayName)
        if (signupResult.isSuccess) {
            signupResult.getOrNull()?.let { user ->
                firestoreSource.createUserProfileDocument(user.uid, user.displayName, user.email)
            }
        }
        return signupResult
    }
    /**
     * Le dice a la fuente de datos que cierre la sesión.
     */
    override fun logout() = firebaseAuthSource.logout()

    /**
     * Llama a la fuente de datos para actualizar la URL de la foto de perfil.
     */
    override suspend fun updateUserProfilePicture(photoUrl: String): Result<Unit> {
        return firebaseAuthSource.updateProfilePicture(photoUrl)
    }
}