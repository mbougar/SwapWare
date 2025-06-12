package com.mbougar.swapware.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Representa la valoración que un usuario le da a otro después de una transacción.
 * Guarda quién valora a quién, sobre qué anuncio y con qué puntuación.
 *
 * @property ratedUserId El ID del usuario que recibe la valoración.
 * @property raterUserId El ID del usuario que hace la valoración.
 * @property adId El ID del anuncio relacionado con la transacción.
 * @property conversationId El ID de la conversación donde se acordó la venta.
 * @property ratingValue La puntuación dada, normalmente de 1 a 5.
 * @property timestamp Cuándo se hizo la valoración.
 */
data class UserRating(
    val ratedUserId: String = "",
    val raterUserId: String = "",
    val adId: String = "",
    val conversationId: String = "",
    val ratingValue: Int = 0,
    @ServerTimestamp
    val timestamp: Date? = null,
)