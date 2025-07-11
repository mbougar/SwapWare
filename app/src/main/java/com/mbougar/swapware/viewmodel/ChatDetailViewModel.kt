package com.mbougar.swapware.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.model.UserRating
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

/**
 * Contiene todo el estado para la pantalla de detalle del chat.
 */
data class ChatDetailUiState(
    val chatListItems: List<ChatListItem> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val otherUserDisplayName: String = "",
    val adTitle: String = "",
    val adDetails: Ad? = null,
    val conversationDetails: Conversation? = null,
    val showRatingDialogForUser: String? = null,
    val ratingSubmissionInProgress: Boolean = false,
)

/**
 * Representa un item en la lista del chat. Puede ser un mensaje o un separador de fecha.
 */
sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DateSeparatorItem(val date: String) : ChatListItem()
}

/**
 * Agrupa los mensajes por día y añade separadores de fecha.
 * @param messages La lista de mensajes a agrupar.
 * @return Una lista de ChatListItem con mensajes y fechas.
 */
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

/**
 * Formatea una fecha para el separador, mostrando "Hoy", "Ayer" o la fecha.
 * @param date La fecha a formatear.
 * @return El texto formateado.
 */
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

/**
 * ViewModel para la pantalla de detalle del chat.
 */
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val otherUserDisplayName: String = checkNotNull(savedStateHandle["otherUserDisplayName"])
    private val adTitle: String = checkNotNull(savedStateHandle["adTitle"])

    private val adIdFromConversation: MutableStateFlow<String?> = MutableStateFlow(null)

    // Flujo para el estado de la UI.
    private val _uiState = MutableStateFlow(
        ChatDetailUiState(
            currentUserId = authRepository.getCurrentUser()?.uid,
            otherUserDisplayName = java.net.URLDecoder.decode(otherUserDisplayName, "UTF-8"),
            adTitle = java.net.URLDecoder.decode(adTitle, "UTF-8")
        )
    )
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val convResult = messageRepository.getConversationDetails(conversationId)
            if (convResult.isSuccess) {
                val conversation = convResult.getOrNull()
                _uiState.update { it.copy(conversationDetails = conversation) }
                conversation?.adId?.let { currentAdId ->
                    adIdFromConversation.value = currentAdId
                    val ad = messageRepository.getAdDetailsForConversation(currentAdId)
                    _uiState.update { it.copy(adDetails = ad) }
                }
            } else {
                _uiState.update { it.copy(error = "Failed to load conversation details.")}
            }
        }
        loadMessages()
    }

    /**
     * Carga los mensajes de la conversación.
     */
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

    /**
     * Se llama cuando el usuario cambia el texto en el campo de entrada.
     * @param newText El nuevo texto.
     */
    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    /**
     * Envía el mensaje escrito por el usuario.
     */
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

    /**
     * Marca el anuncio como vendido al otro usuario del chat.
     */
    fun markAdAsSoldToOtherUser() {
        val ad = _uiState.value.adDetails ?: return
        val currentConversation = _uiState.value.conversationDetails ?: return
        val currentUserId = _uiState.value.currentUserId ?: return

        if (ad.sellerId != currentUserId || ad.sold) {
            _uiState.update { it.copy(error = "Cannot mark ad as sold.")}
            return
        }

        val otherParticipantId = currentConversation.participantIds.find { it != currentUserId }
        if (otherParticipantId == null) {
            _uiState.update { it.copy(error = "Could not identify buyer in conversation.")}
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = messageRepository.markAdAsSoldViaConversation(
                conversationId = currentConversation.id,
                adId = ad.id,
                buyerIdInChat = otherParticipantId,
                sellerId = ad.sellerId
            )
            if (result.isSuccess) {
                val updatedAd = messageRepository.getAdDetailsForConversation(ad.id)
                val updatedConv = messageRepository.getConversationDetails(currentConversation.id).getOrNull()
                _uiState.update { it.copy(isLoading = false, adDetails = updatedAd, conversationDetails = updatedConv, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to mark as sold.") }
            }
        }
    }

    /**
     * Muestra el diálogo para valorar a un usuario.
     * @param userIdToRate El ID del usuario que se va a valorar.
     */
    fun onAttemptToRateUser(userIdToRate: String) {
        _uiState.update { it.copy(showRatingDialogForUser = userIdToRate) }
    }

    /**
     * Cierra el diálogo de valoración.
     */
    fun onDismissRatingDialog() {
        _uiState.update { it.copy(showRatingDialogForUser = null) }
    }

    /**
     * Envía la valoración de un usuario.
     * @param ratingValue La puntuación (de 1 a 5).
     */
    fun submitRating(ratingValue: Int) {
        val ratedUserId = _uiState.value.showRatingDialogForUser ?: return
        val raterUserId = _uiState.value.currentUserId ?: return
        val ad = _uiState.value.adDetails ?: return
        val currentConversation = _uiState.value.conversationDetails ?: return

        if (ratingValue < 1 || ratingValue > 5) {
            _uiState.update { it.copy(error = "Invalid rating value.") }
            return
        }

        val isSellerRatingBuyer = ad.sellerId == raterUserId && ratedUserId == currentConversation.adSoldToParticipantId

        if (isSellerRatingBuyer && currentConversation.sellerRatedBuyerForAd) {
            _uiState.update { it.copy(error = "You have already rated the buyer for this transaction.", showRatingDialogForUser = null) }
            return
        }
        if (!isSellerRatingBuyer && ad.sellerId == ratedUserId && raterUserId == currentConversation.adSoldToParticipantId && currentConversation.buyerRatedSellerForAd) {
            _uiState.update { it.copy(error = "You have already rated the seller for this transaction.", showRatingDialogForUser = null) }
            return
        }


        val userRating = UserRating(
            ratedUserId = ratedUserId,
            raterUserId = raterUserId,
            adId = ad.id,
            conversationId = conversationId,
            ratingValue = ratingValue
        )

        viewModelScope.launch {
            _uiState.update { it.copy(ratingSubmissionInProgress = true) }
            val result = messageRepository.submitRatingAndUpdateProfile(
                rating = userRating,
                conversationId = currentConversation.id,
                isSellerSubmitting = isSellerRatingBuyer
            )

            if (result.isSuccess) {
                val updatedConv = messageRepository.getConversationDetails(currentConversation.id).getOrNull()
                _uiState.update { it.copy(ratingSubmissionInProgress = false, showRatingDialogForUser = null, conversationDetails = updatedConv, error = null) }
            } else {
                _uiState.update { it.copy(ratingSubmissionInProgress = false, error = result.exceptionOrNull()?.message ?: "Failed to submit rating.") }
            }
        }
    }

    /**
     * Limpia el mensaje de error del estado de la UI.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}