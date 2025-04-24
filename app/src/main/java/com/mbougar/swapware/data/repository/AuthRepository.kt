package com.mbougar.swapware.data.repository

interface AuthRepository {
    fun getCurrentUser(): com.google.firebase.auth.FirebaseUser?
    fun isUserLoggedIn(): Boolean
    suspend fun login(email: String, pass: String): Result<com.google.firebase.auth.FirebaseUser>
    suspend fun signup(email: String, pass: String): Result<com.google.firebase.auth.FirebaseUser>
    fun logout()
}