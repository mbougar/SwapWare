package com.mbougar.swapware.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Se encarga de todo lo relacionado con Firebase Storage.
 * Básicamente, la usamos para subir archivos, como las fotos de los anuncios.
 *
 * @param storage La instancia de Firebase Storage que nos pasa Dagger/Hilt.
 */
@Singleton
class FirebaseStorageSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Sube una imagen a Firebase Storage.
     * Le damos la imagen desde el móvil y nos devuelve la URL pública para poder verla.
     *
     * @param imageUri La dirección (URI) de la imagen en el dispositivo.
     * @param pathPrefix La carpeta dentro de Firebase Storage donde se guardará (por defecto, "ad_images").
     * @return Un `Result` que contiene la URL de la imagen si todo va bien, o un error si falla.
     */
    suspend fun uploadImage(imageUri: Uri, pathPrefix: String = "ad_images"): Result<String> {
        return try {
            val storageRef = storage.reference
            val imageRef = storageRef.child("$pathPrefix/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}