package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Guarda el estado de la pantalla de detalle de un anuncio.
 */
data class AdDetailUiState(
    val ad: Ad? = null,
    val currentUserId: String? = null,
    val isOwnAd: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isInitiatingConversation: Boolean = false,
    val conversationError: String? = null,
    val navigateToConversationId: String? = null
)

/**
 * ViewModel para la pantalla de detalle de un anuncio.
 */
@HiltViewModel
class AdDetailViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val adId: String = checkNotNull(savedStateHandle["adId"])

    private val _uiState = MutableStateFlow(AdDetailUiState())
    val uiState: StateFlow<AdDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentUserId = authRepository.getCurrentUser()?.uid) }
        loadAdDetails()
    }

    /**
     * Carga los detalles del anuncio desde el repositorio.
     */
    private fun loadAdDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ad = adRepository.getAdById(adId)
                if (ad != null) {
                    _uiState.update { it.copy(ad = ad, isLoading = false, isOwnAd = ad.sellerId == it.currentUserId && it.currentUserId != null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Ad not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load ad details") }
            }
        }
    }

    /**
     * Cambia el estado de favorito del anuncio.
     */
    fun toggleFavorite() {
        val currentAd = _uiState.value.ad ?: return
        if (_uiState.value.isOwnAd) return
        viewModelScope.launch {
            _uiState.update { it.copy(ad = currentAd.copy(isFavorite = !currentAd.isFavorite)) }
            adRepository.toggleFavorite(currentAd.id, !currentAd.isFavorite)
        }
    }

    /**
     * Inicia una conversación con el vendedor del anuncio.
     */
    fun initiateConversation() {
        val currentAd = _uiState.value.ad ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isInitiatingConversation = true, conversationError = null, navigateToConversationId = null) }
            val result = messageRepository.findOrCreateConversationForAd(currentAd)

            result.onSuccess { conversationId ->
                _uiState.update { it.copy(isInitiatingConversation = false, navigateToConversationId = conversationId) }
            }.onFailure { error ->
                _uiState.update { it.copy(isInitiatingConversation = false, conversationError = error.message ?: "Failed to start conversation") }
            }
        }
    }

    /**
     * Resetea los flags de navegación y error una vez que se han gestionado.
     */
    fun navigationOrErrorHandled() {
        _uiState.update { it.copy(navigateToConversationId = null, conversationError = null) }
    }
}