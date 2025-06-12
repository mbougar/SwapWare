package com.mbougar.swapware.data.model

/**
 * Almacena la información pública del perfil de un usuario.
 * Estos son los datos que otros usuarios pueden ver.
 *
 * @property userId El ID único del usuario.
 * @property displayName El nombre que el usuario ha elegido para mostrar.
 * @property profilePictureUrl La URL de su foto de perfil.
 * @property totalRatingPoints La suma de todas las estrellas que ha recibido.
 * @property numberOfRatings El número total de valoraciones que ha recibido.
 * @property averageRating La media de sus valoraciones (calculada a partir de los dos anteriores).
 */
data class UserProfileData(
    val userId: String = "",
    val displayName: String? = null,
    val profilePictureUrl: String? = null,
    var totalRatingPoints: Long = 0,
    var numberOfRatings: Long = 0,
    var averageRating: Float = 0.0f
)