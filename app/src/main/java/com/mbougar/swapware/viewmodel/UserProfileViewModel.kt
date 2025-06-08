package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.remote.FirestoreSource
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileScreenUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userDisplayName: String? = null,
    val profilePictureUrl: String? = null,
    val averageRating: Float? = null,
    val numberOfRatings: Long? = null,
    val userAds: List<Ad> = emptyList()
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val adRepository: AdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileScreenUiState())
    val uiState: StateFlow<UserProfileScreenUiState> = _uiState.asStateFlow()

    private val _currentLoggedInUserId = MutableStateFlow<String?>(null)
    val currentLoggedInUserId: StateFlow<String?> = _currentLoggedInUserId.asStateFlow()

    init {
        _currentLoggedInUserId.value = authRepository.getCurrentUser()?.uid
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserProfileScreenUiState(isLoading = true)

            val profileResult = firestoreSource.getUserProfile(userId)
            val userProfileData = if (profileResult.isSuccess) profileResult.getOrNull() else null

            adRepository.getAdsByUserId(userId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load user ads: ${e.message}") }
                }
                .collect { adsResult ->
                    if (adsResult.isSuccess) {
                        val ads = adsResult.getOrNull()?.filter { !it.sold } ?: emptyList()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                userDisplayName = userProfileData?.displayName,
                                profilePictureUrl = userProfileData?.profilePictureUrl,
                                averageRating = userProfileData?.averageRating,
                                numberOfRatings = userProfileData?.numberOfRatings,
                                userAds = ads
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load user ads: ${adsResult.exceptionOrNull()?.message}",
                                userDisplayName = userProfileData?.displayName,
                                profilePictureUrl = userProfileData?.profilePictureUrl,
                                averageRating = userProfileData?.averageRating,
                                numberOfRatings = userProfileData?.numberOfRatings
                            )
                        }
                    }
                }
            if (profileResult.isFailure && _uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load user profile: ${profileResult.exceptionOrNull()?.message}") }
            }
        }
    }

    fun toggleFavorite(adId: String, loggedInUserId: String?) {
        if (loggedInUserId == null) return

        viewModelScope.launch {
            val currentAds = _uiState.value.userAds
            val adToToggle = currentAds.find { it.id == adId }

            adToToggle?.let {
                if (it.sellerId == loggedInUserId) return@launch

                val newFavStatus = !it.isFavorite
                _uiState.update { currentState ->
                    currentState.copy(
                        userAds = currentState.userAds.map { ad ->
                            if (ad.id == adId) ad.copy(isFavorite = newFavStatus) else ad
                        }
                    )
                }
                adRepository.toggleFavorite(adId, newFavStatus)
            }
        }
    }
}