package com.mbougar.swapware.data.repository

import android.util.Log
import com.mbougar.swapware.data.local.AdDao
import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.NewAdData
import com.mbougar.swapware.data.remote.FirebaseAuthSource
import com.mbougar.swapware.data.remote.FirebaseStorageSource
import com.mbougar.swapware.data.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de anuncios.
 * Se encarga de mezclar los datos de los anuncios que vienen de la red (Firestore)
 * con los que tenemos guardados en el móvil (base de datos Room), como los favoritos.
 *
 * @param adDao El DAO para acceder a la base de datos local de anuncios.
 * @param firestoreSource Para hablar con la base de datos de Firestore.
 * @param storageSource Para subir las imágenes de los anuncios.
 * @param firebaseAuthSource Para saber quién es el usuario actual.
 */
@Singleton
class AdRepositoryImpl @Inject constructor(
    private val adDao: AdDao,
    private val firestoreSource: FirestoreSource,
    private val storageSource: FirebaseStorageSource,
    private val firebaseAuthSource: FirebaseAuthSource
) : AdRepository {

    /**
     * Publica un anuncio nuevo.
     * El proceso es: subir la imagen si hay, crear el objeto Ad, guardarlo en Firestore
     * y, si todo va bien, guardarlo también en la base de datos local.
     */
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
                title = adData.title,
                description = adData.description,
                price = adData.price,
                category = adData.category,
                sellerId = currentUser.uid,
                sellerEmail = currentUser.email ?: "N/A",
                sellerDisplayName = currentUser.displayName ?: "Anonymous",
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                sellerLocation = adData.sellerLocation,
                sellerLatitude = adData.sellerLatitude,
                sellerLongitude = adData.sellerLongitude
            )

            val firestoreResult: Result<String> = firestoreSource.saveAd(ad)

            return@withContext if (firestoreResult.isSuccess) {
                val savedAdId = firestoreResult.getOrNull()
                if (savedAdId != null) {
                    val adToSaveInRoom = ad.copy(id = savedAdId)
                    adDao.insertAd(adToSaveInRoom)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to get Ad ID from Firestore save operation despite success."))
                }
            } else {
                Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Unknown error saving ad to Firestore."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la lista de anuncios.
     * Usa una estrategia de "cache primero":
     * 1. Envía los anuncios que hay en la base de datos local al momento.
     * 2. Se queda escuchando a Firestore por si hay cambios.
     * 3. Cuando hay cambios, los mezcla con los datos locales (para no perder los favoritos)
     *    y actualiza la base de datos local.
     */
    override fun getAds(): Flow<Result<List<Ad>>> = channelFlow {
        try {
            val localAds = adDao.getAllAds().first()
            send(Result.success(localAds))
        } catch (e: Exception) {
            Log.w("AdRepository", "Error fetching initial ads from local cache", e)
        }

        try {
            firestoreSource.getAdsStream()
                .distinctUntilChanged()
                .collect { remoteAds ->
                    val currentLocalAds = adDao.getAllAds().first()
                    val localFavorites = currentLocalAds.filter { it.isFavorite }.map { it.id }.toSet()

                    val updatedAds = remoteAds.map { remoteAd ->
                        remoteAd.copy(isFavorite = localFavorites.contains(remoteAd.id))
                    }

                    adDao.deleteAllAds()
                    adDao.insertAds(updatedAds)

                    send(Result.success(adDao.getAllAds().first()))
                }
        } catch (e: Exception) {
            Log.e("AdRepository", "Error in Firestore ads stream", e)
            send(Result.failure(e))
        }
    }.catch { e ->
        Log.e("AdRepository", "Exception in getAds channelFlow", e)
        emit(Result.failure(Exception("Failed to observe ads", e)))
    }.flowOn(Dispatchers.IO) // Fuerzo que todas las operaciones se realizen en el hilo IO

    /**
     * Obtiene solo los anuncios marcados como favoritos desde la base de datos local.
     */
    override fun getFavoriteAds(): Flow<List<Ad>> {
        return adDao.getFavoriteAds().flowOn(Dispatchers.IO)
    }

    /**
     * Actualiza un anuncio en la base de datos local.
     */
    override suspend fun updateAd(ad: Ad): Result<Unit> = withContext(Dispatchers.IO) {
        adDao.updateAd(ad)
        Result.success(Unit)
    }

    /**
     * Borra un anuncio. Lo borra primero de Firestore y, si se borra bien,
     * lo borra también de la base de datos local.
     */
    override suspend fun deleteAd(ad: Ad): Result<Unit> = withContext(Dispatchers.IO) {
        val result = firestoreSource.deleteAd(ad.id)
        if(result.isSuccess) {
            adDao.deleteAdById(ad.id)
        }
        result
    }

    /**
     * Marca o desmarca un anuncio como favorito.
     * Esta operación solo afecta a la base de datos local (Room).
     */
    override suspend fun toggleFavorite(adId: String, isFavorite: Boolean): Unit = withContext(Dispatchers.IO) {
        val ad = adDao.getAdById(adId)
        ad?.let {
            adDao.updateAd(it.copy(isFavorite = isFavorite))
        }
    }

    /**
     * Fuerza una recarga de todos los anuncios desde Firestore para actualizar la caché local.
     * Es útil para el "pull to refresh".
     */
    override suspend fun refreshAds(): Result<Unit> = withContext(Dispatchers.IO) {

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

    /**
     * Busca un único anuncio por su ID en la base de datos local.
     */
    override suspend fun getAdById(adId: String): Ad? = withContext(Dispatchers.IO) { adDao.getAdById(adId) }

    /**
     * Obtiene los anuncios de un usuario concreto desde Firestore.
     * También comprueba la base de datos local para marcar si alguno de esos
     * anuncios está en la lista de favoritos del usuario actual.
     */
    override fun getAdsByUserId(userId: String): Flow<Result<List<Ad>>> = channelFlow<Result<List<Ad>>> {
        try {
            firestoreSource.getAdsByUserIdStream(userId)
                .distinctUntilChanged()
                .collect { remoteUserAds ->
                    val localAds = adDao.getAllAds().first()
                    val favoriteIds = localAds.filter { it.isFavorite }.map { it.id }.toSet()

                    val userAdsWithFavStatus = remoteUserAds.map { ad ->
                        ad.copy(isFavorite = favoriteIds.contains(ad.id))
                    }
                    send(Result.success(userAdsWithFavStatus))
                }
        } catch (e: Exception) {
            send(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(Exception("Failed to observe user's ads", e)))
    }.flowOn(Dispatchers.IO)

    /**
     * Marca un anuncio como vendido.
     * Lo actualiza en Firestore y luego en la base de datos local (Room).
     */
    override suspend fun markAdAsSold(adId: String, buyerUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val soldTime = System.currentTimeMillis()
        val firestoreResult = firestoreSource.markAdAsSold(adId, buyerUserId, soldTime)
        if (firestoreResult.isSuccess) {
            val ad = adDao.getAdById(adId)
            ad?.let {
                adDao.updateAd(it.copy(sold = true, soldToUserId = buyerUserId, soldTimestamp = soldTime))
            }
        }
        firestoreResult
    }
}