package com.mbougar.swapware.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageSource @Inject constructor(
    private val storage: FirebaseStorage
) {
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