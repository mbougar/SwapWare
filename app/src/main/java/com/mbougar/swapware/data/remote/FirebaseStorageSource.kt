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
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference
            // Quiza podria crear el path con el user id: images/userid/uuid.jpg
            // val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
            val imageRef = storageRef.child("ad_images/${UUID.randomUUID()}")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}