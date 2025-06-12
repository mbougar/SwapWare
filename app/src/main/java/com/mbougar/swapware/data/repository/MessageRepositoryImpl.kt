package com.mbougar.swapware.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.model.UserRating
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de mensajes y conversaciones.
 * Conecta la lógica de la app con Firestore para todo lo relacionado con los chats.
 *
 * @param firestoreSource Para leer y escribir en la base de datos de Firestore.
 * @param authSource Para saber quién es el usuario logueado.
 * @param adRepository Para obtener detalles de los anuncios relacionados con los chats.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val authSource: FirebaseAuthSource,
    private val adRepository: AdRepository
) : MessageRepository {

    /**
     * Obtiene un flujo en tiempo real con la lista de conversaciones del usuario.
     */
    override fun getConversationsStream(): Flow<Result<List<Conversation>>> = flow {
        val userId = authSource.getCurrentUser()?.uid
        if (userId == null) {
            emit(Result.failure(Exception("User not logged in")))
            return@flow
        }

        firestoreSource.getConversationsStreamForUser(userId)
            .collect { conversations ->
                emit(Result.success(conversations))
            }
    }.catch { e ->
        Log.e("MessageRepo", "Error in conversations stream", e)
        emit(Result.failure(Exception("Failed to process conversation update: ${e.message}", e)))
    }.flowOn(Dispatchers.IO)

    /**
     * Obtiene un flujo en tiempo real con los mensajes de una conversación específica.
     */
    override fun getMessagesStream(conversationId: String): Flow<Result<List<Message>>> = flow {
        firestoreSource.getMessagesStream(conversationId)
            .collect { messages ->
                emit(Result.success(messages))
            }
    }.catch { e ->
        emit(Result.failure(Exception("Failed to load messages", e)))
    }.flowOn(Dispatchers.IO)

    /**
     * Envía un mensaje nuevo a una conversación.
     * Primero lo guarda en la colección de mensajes y luego actualiza el
     * resumen de la conversación (último mensaje y fecha).
     */
    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = authSource.getCurrentUser()
        if (currentUser == null || text.isBlank()) {
            return@withContext Result.failure(Exception("User not logged in or message empty"))
        }

        val message = Message(
            conversationId = conversationId,
            senderId = currentUser.uid,
            senderEmail = currentUser.email ?: "N/A",
            senderDisplayName = currentUser.displayName ?: "Anonymous",
            text = text.trim(),
        )

        val sendResult = firestoreSource.sendMessage(conversationId, message)
        if (sendResult.isFailure) {
            return@withContext sendResult
        }

        val timestamp = Timestamp.now()
        val updateResult = firestoreSource.updateConversationSummary(
            conversationId = conversationId,
            lastMessageSnippet = message.text,
            timestamp = timestamp
        )

        return@withContext Result.success(Unit)
    }

    /**
     * Obtiene todos los detalles de un objeto de conversación.
     */
    override suspend fun getConversationDetails(conversationId: String): Result<Conversation> = withContext(Dispatchers.IO) {
        firestoreSource.getConversationDetails(conversationId)
    }

    /**
     * Busca si ya existe un chat para un anuncio entre el usuario actual y el vendedor.
     * Si no existe, lo crea.
     * @return El ID de la conversación, ya sea la encontrada o la nueva.
     */
    override suspend fun findOrCreateConversationForAd(ad: Ad): Result<String> = withContext(Dispatchers.IO) {
        val currentUser = authSource.getCurrentUser()
        if (currentUser == null) {
            return@withContext Result.failure(Exception("User not logged in"))
        }

        val currentUserId = currentUser.uid
        val sellerId = ad.sellerId

        if (currentUserId == sellerId) {
            return@withContext Result.failure(Exception("Cannot start conversation with yourself"))
        }

        val findResult = firestoreSource.findConversation(ad.id, currentUserId, sellerId)

        if (findResult.isSuccess) {
            val existingConversation = findResult.getOrNull()
            if (existingConversation != null) {
                return@withContext Result.success(existingConversation.id)
            } else {
                val currentUserDisplayName = currentUser.displayName ?: "Anonymous"
                val sellerDisplayName = ad.sellerDisplayName

                val newConversation = Conversation(
                    adId = ad.id,
                    adTitle = ad.title,
                    participantIds = listOf(currentUser.uid, ad.sellerId).sorted(),
                    participantEmails = if (currentUser.uid < ad.sellerId) {
                        listOf(currentUser.email ?: "N/A", ad.sellerEmail)
                    } else {
                        listOf(ad.sellerEmail, currentUser.email ?: "N/A")
                    },
                    participantDisplayNames = if (currentUser.uid < ad.sellerId) {
                        listOf(currentUserDisplayName, sellerDisplayName)
                    } else {
                        listOf(sellerDisplayName, currentUserDisplayName)
                    },
                    lastMessageSnippet = null,
                    lastMessageTimestamp = java.util.Date()
                )
                val createResult = firestoreSource.createConversation(newConversation)

                return@withContext createResult
            }
        } else {
            return@withContext Result.failure(findResult.exceptionOrNull() ?: Exception("Failed to search for conversations"))
        }
    }

    /**
     * Usa el AdRepository para obtener los detalles de un anuncio a partir de su ID.
     * Es útil para mostrar la info del anuncio dentro de la pantalla del chat.
     */
    override suspend fun getAdDetailsForConversation(adId: String): Ad? {
        return adRepository.getAdById(adId)
    }

    /**
     * Marca un anuncio como vendido a través de una conversación.
     * Primero actualiza el anuncio y luego actualiza la conversación para reflejarlo.
     */
    override suspend fun markAdAsSoldViaConversation(
        conversationId: String,
        adId: String,
        buyerIdInChat: String,
        sellerId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val markAdResult = adRepository.markAdAsSold(adId, buyerIdInChat)
        if (markAdResult.isFailure) {
            return@withContext markAdResult
        }
        return@withContext firestoreSource.updateConversationAdSoldStatus(conversationId, buyerIdInChat)
    }

    /**
     * Procesa el envío de una valoración.
     * Guarda la valoración en la base de datos y luego actualiza la conversación
     * para marcar que el usuario ya ha valorado.
     */
    override suspend fun submitRatingAndUpdateProfile(
        rating: UserRating,
        conversationId: String,
        isSellerSubmitting: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val submitRatingResult = firestoreSource.submitRating(rating)
        if (submitRatingResult.isFailure) {
            return@withContext submitRatingResult
        }

        val convUpdateResult = firestoreSource.updateConversationRatingStatus(conversationId, isSellerSubmitting)
        if (convUpdateResult.isFailure) {
            Log.w("MessageRepo", "Failed to update conversation rating status, but rating was submitted.")
        }

        return@withContext if (submitRatingResult.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(submitRatingResult.exceptionOrNull() ?: Exception("Failed to submit rating document."))
        }
    }
}