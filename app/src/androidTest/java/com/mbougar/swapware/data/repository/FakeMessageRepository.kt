package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeMessageRepository @Inject constructor() : MessageRepository {
    override fun getConversationsStream(): Flow<Result<List<Conversation>>> = flowOf(Result.success(emptyList()))
    override fun getMessagesStream(conversationId: String): Flow<Result<List<Message>>> = flowOf(Result.success(emptyList()))
    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = Result.success(Unit)
    override suspend fun getConversationDetails(conversationId: String): Result<Conversation> = Result.success(Conversation())
    override suspend fun findOrCreateConversationForAd(ad: Ad): Result<String> = Result.success("fake_conv_id")
    override suspend fun getAdDetailsForConversation(adId: String): Ad? = null
    override suspend fun markAdAsSoldViaConversation(conversationId: String, adId: String, buyerIdInChat: String, sellerId: String): Result<Unit> = Result.success(Unit)
    override suspend fun submitRatingAndUpdateProfile(rating: UserRating, conversationId: String, isSellerSubmitting: Boolean): Result<Unit> = Result.success(Unit)
}
