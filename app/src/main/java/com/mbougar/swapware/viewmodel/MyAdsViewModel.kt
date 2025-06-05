package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAdsUiState(
    val ads: List<Ad> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String? = null
)

@HiltViewModel
class MyAdsViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyAdsUiState())
    val uiState: StateFlow<MyAdsUiState> = _uiState.asStateFlow()

    init {
        val userId = authRepository.getCurrentUser()?.uid
        _uiState.value = _uiState.value.copy(currentUserId = userId)
        if (userId != null) {
            loadMyAds(userId)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "User not logged in.")
        }
    }

    private fun loadMyAds(userId: String) {
        viewModelScope.launch {
            adRepository.getAdsByUserId(userId)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error loading ads") } }
                .collect { result ->
                    result.onSuccess { ads ->
                        _uiState.update { it.copy(ads = ads, isLoading = false) }
                    }.onFailure { throwable ->
                        _uiState.update { it.copy(isLoading = false, error = throwable.message ?: "Failed to load ads") }
                    }
                }
        }
    }

    fun toggleFavorite(adId: String, currentUserId: String?) {
        val adToToggle = _uiState.value.ads.find { it.id == adId }
        if (adToToggle != null && adToToggle.sellerId == currentUserId) {
            return
        }

        viewModelScope.launch {
            val currentAds = _uiState.value.ads
            val ad = currentAds.find { it.id == adId }
            ad?.let {
                val newFavStatus = !it.isFavorite
                _uiState.update { currentState ->
                    currentState.copy(
                        ads = currentState.ads.map { adItem ->
                            if (adItem.id == adId) adItem.copy(isFavorite = newFavStatus) else adItem
                        }
                    )
                }
                adRepository.toggleFavorite(adId, newFavStatus)
            }
        }
    }
}