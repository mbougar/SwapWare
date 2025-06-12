package com.mbougar.swapware.data.model

import android.net.Uri

/**
 * Es una clase de ayuda para guardar temporalmente los datos de un anuncio nuevo.
 * Se usa para recoger toda la información del formulario antes de crear el objeto `Ad` final.
 *
 * @property title El título del anuncio.
 * @property description La descripción.
 * @property price El precio.
 * @property category La categoría.
 * @property imageUri La URI de la imagen seleccionada del móvil (si la hay).
 * @property sellerLocation La ubicación seleccionada por el vendedor.
 * @property sellerLatitude La latitud de la ubicación.
 * @property sellerLongitude La longitud de la ubicación.
 */
data class NewAdData(
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUri: Uri?,
    val sellerLocation: String?,
    val sellerLatitude: Double?,
    val sellerLongitude: Double?
)