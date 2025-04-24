package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getConversationsStream(): Flow<Result<List<Conversation>>>
    fun getMessagesStream(conversationId: String): Flow<Result<List<Message>>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>
    suspend fun getConversationDetails(conversationId: String): Result<Conversation>
}