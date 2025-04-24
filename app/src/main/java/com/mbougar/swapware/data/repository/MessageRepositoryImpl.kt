package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.Conversation
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
        emit(Result.failure(Exception("Failed to load conversations", e)))
    }.flowOn(Dispatchers.IO)
}