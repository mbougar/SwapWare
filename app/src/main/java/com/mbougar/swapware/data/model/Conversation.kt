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
    val participantDisplayNames: List<String> = emptyList(),
    val lastMessageSnippet: String? = null,
    @ServerTimestamp
    val lastMessageTimestamp: Date? = null,
    var adIsSoldInThisConversation: Boolean = false,
    var adSoldToParticipantId: String? = null,
    var sellerRatedBuyerForAd: Boolean = false,
    var buyerRatedSellerForAd: Boolean = false
    // TODO Quiza puedo añadir un contador de mensajes sin leer
    // val unreadCount: Map<String, Int> = emptyMap()
)