package com.mbougar.swapware.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots // For Flow support
import com.google.firebase.firestore.ktx.toObject // For object mapping
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val adsCollection = firestore.collection("ads")
    private val conversationsCollection = firestore.collection("conversations")

    // Flow nos da real-time update
    fun getAdsStream(): Flow<List<Ad>> {
        return adsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots() // Flow<QuerySnapshot> es deprecado, quiza usar KTX
            .map { snapshot ->
                snapshot.toObjects(Ad::class.java)
            }
    }

    suspend fun saveAd(ad: Ad): Result<Unit> {
        return try {
            // usamos la id del Ad si la tiene (por que es una update) o generamos una nueva
            adsCollection.document(ad.id.ifEmpty { adsCollection.document().id }).set(ad).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAd(adId: String): Result<Unit> {
        return try {
            adsCollection.document(adId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtenemos todas las comversaciones de un usuario
    fun getConversationsStreamForUser(userId: String): Flow<List<Conversation>> {
        return conversationsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java)
            }
    }

    fun getMessagesStream(conversationId: String): Flow<List<Message>> {
        return conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Message::class.java)
            }
    }

    suspend fun sendMessage(conversationId: String, message: Message): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .add(message)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConversationSummary(
        conversationId: String,
        lastMessageSnippet: String,
        timestamp: Timestamp
    ): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .update(
                    mapOf(
                        "lastMessageSnippet" to lastMessageSnippet,
                        "lastMessageTimestamp" to timestamp
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationDetails(conversationId: String): Result<Conversation> {
        return try {
            val doc = conversationsCollection.document(conversationId).get().await()
            val conversation = doc.toObject<Conversation>()
            if (conversation != null) {
                Result.success(conversation)
            } else {
                Result.failure(Exception("Conversation not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConversation(conversation: Conversation): Result<String> {
        return try {
            val documentRef = conversationsCollection.add(conversation).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findConversation(adId: String, user1Id: String, user2Id: String): Result<Conversation?> {
        return try {
            val participantsSorted = listOf(user1Id, user2Id).sorted()

            val querySnapshot = conversationsCollection
                .whereEqualTo("adId", adId)
                .whereEqualTo("participantIds", participantsSorted)
                .limit(1)
                .get()
                .await()

            val conversation = querySnapshot.documents.firstOrNull()?.toObject(Conversation::class.java)
            Result.success(conversation)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO Funciones a implementar
    /*
    suspend fun createConversation(conversation: Conversation): Result<String> {  }
    suspend fun sendMessage(conversationId: String, message: Message): Result<Unit> {  }
    fun getMessagesStream(conversationId: String): Flow<List<Message>> {  }
    suspend fun updateConversationSummary(conversationId: String, lastMessage: Message) {  }
    */

    // TODO () Implementar tambien fetching de user profiles, messages...
}