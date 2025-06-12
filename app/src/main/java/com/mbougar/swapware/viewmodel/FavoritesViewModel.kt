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

/**
 * Guarda el estado de la pantalla de favoritos.
 */
data class FavoritesUiState(
    val favoriteAds: List<Ad> = emptyList(),
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel para la pantalla de Favoritos.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentUserId = authRepository.getCurrentUser()?.uid) }
        loadFavoriteAds()
    }

    /**
     * Carga la lista de anuncios que el usuario ha marcado como favoritos.
     */
    private fun loadFavoriteAds() {
        viewModelScope.launch {
            adRepository.getFavoriteAds()
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true, error = null) }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load favorites")
                }
                .collect { ads ->
                    _uiState.value = FavoritesUiState(favoriteAds = ads, isLoading = false)
                }
        }
    }

    /**
     * Quita un anuncio de la lista de favoritos.
     * @param adId El ID del anuncio a quitar.
     */
    fun removeFromFavorites(adId: String) {
        viewModelScope.launch {
            adRepository.toggleFavorite(adId, false)
        }
    }
}