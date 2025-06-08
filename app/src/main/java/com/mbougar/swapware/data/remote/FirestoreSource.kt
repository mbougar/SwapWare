package com.mbougar.swapware.data.remote

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots // For Flow support
import com.google.firebase.firestore.ktx.toObject // For object mapping
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

@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val adsCollection = firestore.collection("ads")
    private val conversationsCollection = firestore.collection("conversations")
    private val userRatingsCollection = firestore.collection("user_ratings")
    private val usersCollection = firestore.collection("users")

    // Flow nos da real-time update
    fun getAdsStream(): Flow<List<Ad>> {
        return adsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots() // Flow<QuerySnapshot> es deprecado, quiza usar KTX
            .map { snapshot ->
                snapshot.toObjects(Ad::class.java)
            }
    }

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

    suspend fun deleteAd(adId: String): Result<Unit> {
        return try {
            adsCollection.document(adId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAdsByUserIdStream(userId: String): Flow<List<Ad>> {
        return adsCollection
            .whereEqualTo("sellerId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Ad::class.java)
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

    suspend fun getUserProfile(userId: String): Result<UserProfileData?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            Result.success(document.toObject(UserProfileData::class.java))
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error getting user profile $userId", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileRating(userId: String, newTotalPoints: Long, newNumberOfRatings: Long, newAverage: Float): Result<Unit> {
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "totalRatingPoints" to newTotalPoints,
                    "numberOfRatings" to newNumberOfRatings,
                    "averageRating" to newAverage
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSource", "Error updating user profile rating for $userId", e)
            Result.failure(e)
        }
    }

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