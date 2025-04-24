package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val ads: List<Ad> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedCategory: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val adRepository: AdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)

    init {
        loadAds()
    }

    private fun loadAds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adRepository.getAds()
                .combine(_selectedCategory) { adsResult, category ->
                    Pair(adsResult, category)
                }
                .collect { (adsResult, category) ->
                    _uiState.value = _uiState.value.copy(isLoading = false)

                    adsResult.onSuccess { ads ->
                        val filteredAds = if (category != null) {
                            ads.filter { it.category.equals(category, ignoreCase = true) }
                        } else {
                            ads
                        }
                        _uiState.value = _uiState.value.copy(ads = filteredAds, error = null)
                    }.onFailure { throwable ->
                        _uiState.value = _uiState.value.copy(error = throwable.message ?: "An unknown error occurred", ads = emptyList())
                    }
                }
        }
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(adId: String) {
        viewModelScope.launch {
            val currentAds = _uiState.value.ads
            val updatedAds = currentAds.map {
                if (it.id == adId) it.copy(isFavorite = !it.isFavorite) else it
            }
            _uiState.value = _uiState.value.copy(ads = updatedAds)

            val adToUpdate = currentAds.find { it.id == adId }
            adToUpdate?.let {
                adRepository.toggleFavorite(it.id, !it.isFavorite)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = adRepository.refreshAds() // Force refresh
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }
}