package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdDetailUiState(
    val ad: Ad? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AdDetailViewModel @Inject constructor(
    private val adRepository: AdRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val adId: String = checkNotNull(savedStateHandle["adId"])

    private val _uiState = MutableStateFlow(AdDetailUiState())
    val uiState: StateFlow<AdDetailUiState> = _uiState.asStateFlow()

    init {
        loadAdDetails()
    }

    private fun loadAdDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val ad = adRepository.getAdById(adId)

                if (ad != null) {
                    _uiState.value = AdDetailUiState(ad = ad, isLoading = false)
                } else {
                    _uiState.value = AdDetailUiState(isLoading = false, error = "Ad not found")
                    adRepository.refreshAds()
                }
            } catch (e: Exception) {
                _uiState.value = AdDetailUiState(isLoading = false, error = e.message ?: "Failed to load ad details")
            }
        }
    }

    fun toggleFavorite() {
        val currentAd = _uiState.value.ad ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ad = currentAd.copy(isFavorite = !currentAd.isFavorite))
            adRepository.toggleFavorite(currentAd.id, !currentAd.isFavorite)
        }
    }
}