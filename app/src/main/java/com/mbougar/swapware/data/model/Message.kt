package com.mbougar.swapware.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa un único mensaje dentro de una conversación de chat.
 *
 * @property id El identificador único del mensaje.
 * @property conversationId A qué conversación pertenece este mensaje.
 * @property senderId Quién envió el mensaje.
 * @property senderEmail El email del que lo envía.
 * @property senderDisplayName El nombre del que lo envía.
 * @property text El contenido del mensaje.
 * @property timestamp Cuándo se envió el mensaje.
 * @property isRead Si el mensaje ha sido leído o no (actualmente no se usa).
 */
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