package com.mbougar.swapware.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val authSource: FirebaseAuthSource
) : MessageRepository {

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

    override fun getMessagesStream(conversationId: String): Flow<Result<List<Message>>> = flow {
        firestoreSource.getMessagesStream(conversationId)
            .collect { messages ->
                emit(Result.success(messages))
            }
    }.catch { e ->
        emit(Result.failure(Exception("Failed to load messages", e)))
    }.flowOn(Dispatchers.IO)


    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = authSource.getCurrentUser()
        if (currentUser == null || text.isBlank()) {
            return@withContext Result.failure(Exception("User not logged in or message empty"))
        }

        val message = Message(
            conversationId = conversationId,
            senderId = currentUser.uid,
            senderEmail = currentUser.email ?: "N/A",
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

    override suspend fun getConversationDetails(conversationId: String): Result<Conversation> = withContext(Dispatchers.IO) {
        firestoreSource.getConversationDetails(conversationId)
    }

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
                val newConversation = Conversation(
                    adId = ad.id,
                    adTitle = ad.title,
                    participantIds = listOf(currentUserId, sellerId).sorted(),
                    participantEmails = if (currentUserId < sellerId) {
                        listOf(currentUser.email ?: "N/A", ad.sellerEmail)
                    } else {
                        listOf(ad.sellerEmail, currentUser.email ?: "N/A")
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
}