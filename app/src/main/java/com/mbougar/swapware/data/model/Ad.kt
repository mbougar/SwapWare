package com.mbougar.swapware.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

/**
 * Representa un anuncio publicado en la aplicación.
 * Contiene toda la información sobre el producto en venta.
 *
 * @property id El identificador único del anuncio.
 * @property title El título del anuncio.
 * @property description Una descripción más larga sobre el producto.
 * @property price El precio del producto.
 * @property category La categoría a la que pertenece (ej: "GPU", "CPU").
 * @property sellerId El ID del usuario que vende el producto.
 * @property sellerEmail El email del vendedor.
 * @property sellerDisplayName El nombre público del vendedor.
 * @property imageUrl La URL de la imagen del producto, si tiene.
 * @property timestamp Cuándo se publicó el anuncio.
 * @property isFavorite Un valor local para saber si el usuario actual lo tiene en favoritos.
 * @property sellerLocation La ubicación del vendedor (ej: "Madrid, Madrid").
 * @property sellerLatitude La latitud para mostrar en un mapa.
 * @property sellerLongitude La longitud para mostrar en un mapa.
 * @property sold Indica si el anuncio ya se ha vendido.
 * @property soldToUserId A quién se le vendió.
 * @property soldTimestamp Cuándo se vendió.
 */
@Entity(tableName = "ads")
data class Ad(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val sellerId: String = "",
    val sellerEmail: String = "",
    val sellerDisplayName: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false, // Local state
    val sellerLocation: String? = null,
    val sellerLatitude: Double? = null,
    val sellerLongitude: Double? = null,
    var sold: Boolean = false,
    val soldToUserId: String? = null,
    val soldTimestamp: Long? = null
)