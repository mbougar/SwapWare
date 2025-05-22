package com.mbougar.swapware.data.repository

import android.net.Uri
import com.mbougar.swapware.data.local.AdDao
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.NewAdData
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirebaseStorageSource
import com.mbougar.swapware.data.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdRepositoryImpl @Inject constructor(
    private val adDao: AdDao,
    private val firestoreSource: FirestoreSource,
    private val storageSource: FirebaseStorageSource,
    private val firebaseAuthSource: FirebaseAuthSource
) : AdRepository {

    override suspend fun addAd(adData: NewAdData): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuthSource.getCurrentUser()
        if (currentUser == null) {
            return@withContext Result.failure(Exception("User not logged in"))
        }

        try {
            var imageUrl: String? = null
            if (adData.imageUri != null) {
                val uploadResult = storageSource.uploadImage(adData.imageUri)
                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull()
                } else {
                    return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
                }
            }

            val ad = Ad(
                // id vacio para que firebase lo genere
                title = adData.title,
                description = adData.description,
                price = adData.price,
                category = adData.category,
                sellerId = currentUser.uid,
                sellerEmail = currentUser.email ?: "N/A",
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                sellerLocation = adData.sellerLocation
            )

            val firestoreResult = firestoreSource.saveAd(ad)

            if (firestoreResult.isSuccess) {
                val adToSaveInRoom = ad.copy(id = (ad.id.ifEmpty { firestoreResult.getOrNull() ?: "" }).toString())
                if(adToSaveInRoom.id.isNotEmpty()) {
                    adDao.insertAd(adToSaveInRoom)
                }
            }
            firestoreResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAds(): Flow<Result<List<Ad>>> = channelFlow {
        send(Result.success(adDao.getAllAds().first()))

        try {
            firestoreSource.getAdsStream()
                .distinctUntilChanged()
                .collect { remoteAds ->
                    val currentLocalAds = adDao.getAllAds().first()
                    val localFavorites = currentLocalAds.filter { it.isFavorite }.map { it.id }.toSet()

                    val updatedAds = remoteAds.map { ad ->
                        ad.copy(isFavorite = localFavorites.contains(ad.id))
                    }

                    adDao.deleteAllAds() // Potencialmente podria solo eliminar los que han cambiado e insertar solo los cambiados?
                    adDao.insertAds(updatedAds)

                    send(Result.success(adDao.getAllAds().first()))
                }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

    }.catch { e ->
        emit(Result.failure(Exception("Failed to observe ads", e)))
    }.flowOn(Dispatchers.IO) // Fuerzo que todas las operaciones se realizen en el hilo IO

    override fun getFavoriteAds(): Flow<List<Ad>> {
        return adDao.getFavoriteAds().flowOn(Dispatchers.IO)
    }

    override suspend fun updateAd(ad: Ad): Result<Unit> = withContext(Dispatchers.IO) {
        adDao.updateAd(ad)
        Result.success(Unit)
    }

    override suspend fun deleteAd(ad: Ad): Result<Unit> = withContext(Dispatchers.IO) {
        val result = firestoreSource.deleteAd(ad.id)
        if(result.isSuccess) {
            adDao.deleteAdById(ad.id)
        }
        result
    }

    override suspend fun toggleFavorite(adId: String, isFavorite: Boolean): Unit = withContext(Dispatchers.IO) {
        val ad = adDao.getAdById(adId)
        ad?.let {
            adDao.updateAd(it.copy(isFavorite = isFavorite))
        }
    }

    override suspend fun refreshAds(): Result<Unit> = withContext(Dispatchers.IO) {
        // Hacer que getAdsStream() se encargue?.
        try {
            val remoteAds = firestoreSource.getAdsStream().first()
            val currentLocalAds = adDao.getAllAds().first()
            val localFavorites = currentLocalAds.filter { it.isFavorite }.map { it.id }.toSet()
            val updatedAds = remoteAds.map { ad -> ad.copy(isFavorite = localFavorites.contains(ad.id)) }
            adDao.deleteAllAds()
            adDao.insertAds(updatedAds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException("Failed to refresh ads from network", e))
        }
    }

    override suspend fun getAdById(adId: String): Ad? = withContext(Dispatchers.IO) { adDao.getAdById(adId) }
}