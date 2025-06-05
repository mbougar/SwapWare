package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.NewAdData
import kotlinx.coroutines.flow.Flow

interface AdRepository {
    fun getAds(): Flow<Result<List<Ad>>>
    fun getFavoriteAds(): Flow<List<Ad>> // TODO decidir si quiero guardar los favoritos en la nube o solo localmente
    suspend fun addAd(adData: NewAdData): Result<Unit>
    suspend fun updateAd(ad: Ad): Result<Unit>
    suspend fun deleteAd(ad: Ad): Result<Unit>
    suspend fun toggleFavorite(adId: String, isFavorite: Boolean)
    suspend fun refreshAds(): Result<Unit>
    suspend fun getAdById(adId: String): Ad?
    fun getAdsByUserId(userId: String): Flow<Result<List<Ad>>>
}