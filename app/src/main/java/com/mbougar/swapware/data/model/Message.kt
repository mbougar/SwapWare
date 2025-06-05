package com.mbougar.swapware.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    @DocumentId
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderEmail: String = "",
    val senderDisplayName: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false
)