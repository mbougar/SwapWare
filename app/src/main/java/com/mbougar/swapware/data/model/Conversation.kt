package com.mbougar.swapware.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa una conversación (un chat) entre dos usuarios sobre un anuncio.
 *
 * @property id El identificador único de la conversación.
 * @property adId El ID del anuncio sobre el que trata el chat.
 * @property adTitle El título del anuncio, para mostrarlo fácilmente.
 * @property participantIds Una lista con los IDs de los dos usuarios del chat.
 * @property participantDisplayNames Los nombres de los dos usuarios.
 * @property lastMessageSnippet El último mensaje enviado, para mostrar un resumen.
 * @property lastMessageTimestamp La fecha del último mensaje, para ordenar los chats.
 * @property adIsSoldInThisConversation Indica si el trato se cerró en esta conversación.
 * @property adSoldToParticipantId Quién fue el comprador en esta conversación.
 * @property sellerRatedBuyerForAd Si el vendedor ya ha valorado al comprador.
 * @property buyerRatedSellerForAd Si el comprador ya ha valorado al vendedor.
 */
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
)