package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.Conversation
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getConversationsStream(): Flow<Result<List<Conversation>>>
    // TODO Funciones para mandar y recibir messages
}