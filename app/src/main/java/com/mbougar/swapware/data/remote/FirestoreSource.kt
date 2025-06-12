package com.mbougar.swapware.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import com.mbougar.swapware.data.model.UserProfileData
import com.mbougar.swapware.data.model.UserRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Es el cerebro que habla con la base de datos Firestore.
 * Todas las lecturas y escrituras de datos (anuncios, chats, perfiles) pasan por aquí.
 *
 * @param firestore La instancia de FirebaseFirestore que nos da Hilt.
 */
@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val adsCollection = firestore.collection("ads")
    private val conversationsCollection = firestore.collection("conversations")
    private val userRatingsCollection = firestore.collection("user_ratings")
    private val usersCollection = firestore.collection("users")

    /**
     * Obtiene un flujo de datos en tiempo real con todos los anuncios.
     * Si alguien añade o cambia un anuncio, esta lista se actualiza sola.
     */
    fun getAdsStream(): Flow<List<Ad>> {
        return adsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Ad::class.java)
            }
    }

    /**
     * Guarda un anuncio nuevo o actualiza uno que ya existe en Firestore.
     *
     * @param ad El objeto Ad que queremos guardar.
     * @return Un `Result` con el ID del anuncio si se ha guardado bien.
     */
    suspend fun saveAd(ad: Ad): Result<String> {
        return try {
            val docRef = if (ad.id.isBlank()) {
                adsCollection.document()
            } else {
                adsCollection.document(ad.id)
            }
            docRef.set(ad.copy(id = docRef.id)).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error saving ad: ${ad.title}", e)
            Result.failure(e)
        }
    }

    /**
     * Borra un anuncio de Firestore usando su ID.
     *
     * @param adId El ID del anuncio a borrar.
     * @return Un `Result` que indica si se ha borrado con éxito.
     */
    suspend fun deleteAd(adId: String): Result<Unit> {
        return try {
            adsCollection.document(adId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los anuncios de un usuario específico en tiempo real.
     *
     * @param userId El ID del usuario del que queremos ver los anuncios.
     */
    fun getAdsByUserIdStream(userId: String): Flow<List<Ad>> {
        return adsCollection
            .whereEqualTo("sellerId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Ad::class.java)
            }
    }

    /**
     * Obtiene todas las conversaciones de un usuario en tiempo real.
     *
     * @param userId El ID del usuario del que queremos ver los chats.
     */
    fun getConversationsStreamForUser(userId: String): Flow<List<Conversation>> {
        return conversationsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java)
            }
    }

    /**
     * Obtiene todos los mensajes de una conversación específica en tiempo real.
     *
     * @param conversationId El ID de la conversación.
     */
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

    /**
     * Guarda un mensaje nuevo en una conversación.
     *
     * @param conversationId El ID del chat donde se añade el mensaje.
     * @param message El objeto Message que se va a guardar.
     * @return Un `Result` que indica si se ha guardado bien.
     */
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

    /**
     * Actualiza el resumen de una conversación (el último mensaje y la fecha).
     * Esto es para mostrarlo en la lista de chats.
     *
     * @param conversationId El ID del chat a actualizar.
     * @param lastMessageSnippet El texto del último mensaje.
     * @param timestamp La fecha y hora del último mensaje.
     */
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

    /**
     * Obtiene todos los detalles de una conversación.
     *
     * @param conversationId El ID de la conversación.
     */
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

    /**
     * Crea un documento nuevo para una conversación en Firestore.
     *
     * @param conversation El objeto Conversation con los datos iniciales.
     * @return Un `Result` con el ID de la nueva conversación.
     */
    suspend fun createConversation(conversation: Conversation): Result<String> {
        return try {
            val documentRef = conversationsCollection.add(conversation).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca si ya existe una conversación entre dos usuarios sobre un anuncio.
     *
     * @param adId El ID del anuncio.
     * @param user1Id El ID del primer usuario.
     * @param user2Id El ID del segundo usuario.
     * @return Un `Result` con la conversación si la encuentra, o `null` si no existe.
     */
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

    /**
     * Marca en una conversación que el anuncio asociado se ha vendido.
     *
     * @param conversationId El ID del chat.
     * @param soldToParticipantId El ID del usuario que compró el producto.
     */
    suspend fun updateConversationAdSoldStatus(
        conversationId: String,
        soldToParticipantId: String
    ): Result<Unit> {
        return try {
            conversationsCollection.document(conversationId).update(
                mapOf(
                    "adIsSoldInThisConversation" to true,
                    "adSoldToParticipantId" to soldToParticipantId
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error updating conversation ad sold status $conversationId", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza una conversación para indicar que uno de los usuarios ya ha dejado una valoración.
     *
     * @param conversationId El ID del chat.
     * @param raterIsSeller `true` si el que valora es el vendedor.
     */
    suspend fun updateConversationRatingStatus(
        conversationId: String,
        raterIsSeller: Boolean
    ): Result<Unit> {
        return try {
            val fieldToUpdate = if (raterIsSeller) "sellerRatedBuyerForAd" else "buyerRatedSellerForAd"
            conversationsCollection.document(conversationId).update(fieldToUpdate, true).await()
            Result.success(Unit)
        } catch (e:Exception) {
            Log.e("FirestoreSource", "Error updating conversation rating status for $conversationId", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el documento de un anuncio para marcarlo como vendido.
     *
     * @param adId El ID del anuncio.
     * @param buyerUserId El ID del comprador.
     * @param soldTime Cuándo se vendió.
     */
    suspend fun markAdAsSold(adId: String, buyerUserId: String, soldTime: Long): Result<Unit> {
        return try {
            adsCollection.document(adId).update(
                mapOf(
                    "sold" to true,
                    "soldToUserId" to buyerUserId,
                    "soldTimestamp" to soldTime
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error marking ad $adId as sold", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda una nueva valoración de un usuario a otro en la base de datos.
     *
     * @param rating El objeto UserRating con todos los detalles de la valoración.
     */
    suspend fun submitRating(rating: UserRating): Result<Unit> {
        return try {
            val ratingDocId = "${rating.raterUserId}_${rating.ratedUserId}_${rating.adId}"
            userRatingsCollection.document(ratingDocId).set(rating).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error submitting rating", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los datos del perfil público de un usuario.
     *
     * @param userId El ID del usuario.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfileData?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            Result.success(document.toObject(UserProfileData::class.java))
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error getting user profile $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Crea el documento de perfil inicial para un usuario cuando se registra.
     *
     * @param userId El ID del nuevo usuario.
     * @param displayName El nombre que eligió al registrarse.
     * @param email El email que usó.
     */
    suspend fun createUserProfileDocument(userId: String, displayName: String?, email: String?): Result<Unit> {
        return try {
            val userProfile = UserProfileData(
                userId = userId,
                displayName = displayName,
            )
            usersCollection.document(userId).set(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error creating user profile for $userId", e)
            Result.failure(e)
        }
    }
}