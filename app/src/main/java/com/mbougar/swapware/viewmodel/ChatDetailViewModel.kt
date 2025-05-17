package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatDetailUiState(
    val chatListItems: List<ChatListItem> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val otherUserEmail: String = "",
    val adTitle: String = ""
)

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DateSeparatorItem(val date: String) : ChatListItem()
}

fun groupMessagesWithDateSeparators(messages: List<Message>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()

    val groupedItems = mutableListOf<ChatListItem>()
    var lastDateHeader: String? = null

    val dateFormatHeader = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()

    for (message in messages) {
        message.timestamp?.let { timestamp ->
            calendar.time = timestamp
            val currentDateHeader = dateFormatHeader.format(timestamp)

            if (currentDateHeader != lastDateHeader) {
                groupedItems.add(ChatListItem.DateSeparatorItem(formatDateSeparator(timestamp)))
                lastDateHeader = currentDateHeader
            }
        }
        groupedItems.add(ChatListItem.MessageItem(message))
    }
    return groupedItems
}

fun formatDateSeparator(date: Date): String {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val messageCal = Calendar.getInstance().apply { time = date }

    return when {
        today.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR) -> "Today"

        yesterday.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"

        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(date)
    }
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val otherUserEmail: String = checkNotNull(savedStateHandle["otherUserEmail"])
    private val adTitle: String = checkNotNull(savedStateHandle["adTitle"])


    private val _uiState = MutableStateFlow(
        ChatDetailUiState(
            currentUserId = authRepository.getCurrentUser()?.uid,
            otherUserEmail = java.net.URLDecoder.decode(otherUserEmail, "UTF-8"),
            adTitle = java.net.URLDecoder.decode(adTitle, "UTF-8")
        )
    )
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessagesStream(conversationId)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { result ->
                    result.onSuccess { messages ->
                        _uiState.update {
                            it.copy(
                                chatListItems = groupMessagesWithDateSeparators(messages),
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update { it.copy(isLoading = false, error = throwable.message) }
                    }
                }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val textToSend = _uiState.value.inputText.trim()
        if (textToSend.isBlank() || _uiState.value.isSending) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val result = messageRepository.sendMessage(conversationId, textToSend)

            if (result.isSuccess) {
                _uiState.update { it.copy(inputText = "", isSending = false) }
            } else {
                _uiState.update { it.copy(isSending = false, error = result.exceptionOrNull()?.message ?: "Failed to send message") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}