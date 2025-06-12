package com.mbougar.swapware.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.remote.FirebaseStorageSource
import com.mbougar.swapware.data.remote.FirestoreSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Guarda el estado de la pantalla de perfil del usuario logueado.
 */
data class ProfileUiState(
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val profilePictureUrl: String? = null,
    val userRating: Float? = null,
    val isLoading: Boolean = false,
    val isUploadingPicture: Boolean = false,
    val pictureUploadError: String? = null,
    val showDeleteAccountDialog: Boolean = false
)

/**
 * ViewModel para la pantalla de perfil del propio usuario.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageSource: FirebaseStorageSource,
    private val firestoreSource: FirestoreSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Carga los datos del perfil del usuario logueado.
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val user = authRepository.getCurrentUser()
            var userProfileData: UserProfileData? = null
            if (user != null) {
                val profileResult = firestoreSource.getUserProfile(user.uid)
                if (profileResult.isSuccess) {
                    userProfileData = profileResult.getOrNull()
                } else {
                    Log.e("ProfileVM", "Failed to load user profile: ${profileResult.exceptionOrNull()?.message}")
                }
            }

            _uiState.value = _uiState.value.copy(
                userEmail = user?.email,
                userDisplayName = userProfileData?.displayName ?: user?.displayName,
                profilePictureUrl = userProfileData?.profilePictureUrl ?: user?.photoUrl?.toString(),
                userRating = userProfileData?.takeIf { it.numberOfRatings > 0 }?.averageRating,
                isLoading = false,
                isUploadingPicture = false,
                pictureUploadError = null
            )
        }
    }

    /**
     * Se llama cuando el usuario selecciona una nueva foto de perfil.
     * @param imageUri La URI de la imagen seleccionada.
     */
    fun onProfilePictureSelected(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingPicture = true, pictureUploadError = null)
            val userId = authRepository.getCurrentUser()?.uid
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isUploadingPicture = false, pictureUploadError = "User not found.")
                return@launch
            }

            val uploadResult = storageSource.uploadImage(imageUri, "profile_pictures/$userId")

            if (uploadResult.isSuccess) {
                val downloadUrl = uploadResult.getOrNull()
                if (downloadUrl != null) {
                    val updateResult = authRepository.updateUserProfilePicture(downloadUrl)
                    if (updateResult.isSuccess) {
                        loadUserProfile()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUploadingPicture = false,
                            pictureUploadError = updateResult.exceptionOrNull()?.message ?: "Failed to update profile."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isUploadingPicture = false, pictureUploadError = "Failed to get download URL.")
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isUploadingPicture = false,
                    pictureUploadError = uploadResult.exceptionOrNull()?.message ?: "Failed to upload image."
                )
            }
        }
    }

    /**
     * Limpia el error de subida de imagen después de mostrarlo.
     */
    fun clearPictureUploadError() {
        _uiState.value = _uiState.value.copy(pictureUploadError = null)
    }

    /**
     * Cierra la sesión del usuario.
     */
    fun logout() {
        authRepository.logout()
    }

    /**
     * Muestra u oculta el diálogo de confirmación para borrar la cuenta.
     * @param show true para mostrar, false para ocultar.
     */
    fun onShowDeleteAccountDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteAccountDialog = show)
    }

    /**
     * Borra la cuenta del usuario. (Actualmente solo cierra sesión).
     */
    fun deleteAccount() {
        viewModelScope.launch {
            Log.d("ProfileViewModel", "Account deletion requested but not yet implemented.")
            _uiState.value = _uiState.value.copy(showDeleteAccountDialog = false)
            logout()
        }
    }
}