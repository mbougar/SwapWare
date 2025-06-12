package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.mbougar.swapware.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Guarda el estado de la autenticación.
 */
data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: FirebaseUser? = null,
    val isLoginSuccess: Boolean = false
)

/**
 * ViewModel para manejar la lógica de autenticación (login, registro, etc.).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(currentUser = authRepository.getCurrentUser()))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Resetea el flag de éxito de login para que no se dispare la navegación de nuevo.
     */
    fun consumeLoginSuccess() {
        _authState.value = _authState.value.copy(isLoginSuccess = false)
    }

    /**
     * Comprueba si el usuario está actualmente logueado.
     * @return true si está logueado, false si no.
     */
    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    /**
     * Inicia sesión con email y contraseña.
     * @param email El email del usuario.
     * @param pass La contraseña del usuario.
     */
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = _authState.value.copy(error = "Email and password cannot be empty.")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null, isLoginSuccess = false)
            val result = authRepository.login(email.trim(), pass.trim())
            _authState.value = _authState.value.copy(
                isLoading = false,
                currentUser = result.getOrNull(),
                error = result.exceptionOrNull()?.message,
                isLoginSuccess = result.isSuccess
            )
        }
    }

    /**
     * Registra un nuevo usuario.
     * @param email El email del nuevo usuario.
     * @param pass La contraseña del nuevo usuario.
     * @param displayName El nombre que mostrará el nuevo usuario.
     */
    fun signup(email: String, pass: String, displayName: String) {
        if (email.isBlank() || pass.isBlank() || displayName.isBlank()) {
            _authState.value = _authState.value.copy(error = "All fields must be filled.")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null, isLoginSuccess = false)
            val result = authRepository.signup(email.trim(), pass.trim(), displayName.trim())
            _authState.value = _authState.value.copy(
                isLoading = false,
                currentUser = result.getOrNull(),
                error = result.exceptionOrNull()?.message,
                isLoginSuccess = result.isSuccess
            )
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() {
        authRepository.logout()
        _authState.value = AuthState()
    }
}