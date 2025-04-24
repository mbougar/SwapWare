package com.mbougar.swapware.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    @DocumentId
    val id: String = "",
    val adId: String = "",
    val adTitle: String = "",
    val participantIds: List<String> = emptyList(),
    val participantEmails: List<String> = emptyList(),
    val lastMessageSnippet: String? = null,
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,
    // TODO Quiza puedo añadir un contador de mensajes sin leer
    // val unreadCount: Map<String, Int> = emptyMap()
)