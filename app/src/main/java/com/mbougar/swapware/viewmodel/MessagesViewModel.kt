package com.mbougar.swapware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Guarda el estado para la pantalla de la lista de mensajes (conversaciones).
 */
data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String? = null
)

/**
 * ViewModel para la pantalla de mensajes.
 */
@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState(currentUserId = authRepository.getCurrentUser()?.uid))
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    /**
     * Carga la lista de conversaciones del usuario actual.
     */
    private fun loadConversations() {
        viewModelScope.launch {
            messageRepository.getConversationsStream()
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "An error occurred") }
                }
                .collect { result ->
                    result.onSuccess { conversations ->
                        _uiState.update {
                            it.copy(
                                conversations = conversations,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to load messages"
                            )
                        }
                    }
                }
        }
    }

    /**
     * Vuelve a cargar las conversaciones, para el "pull to refresh".
     */
    fun refresh() {
        loadConversations()
    }
}