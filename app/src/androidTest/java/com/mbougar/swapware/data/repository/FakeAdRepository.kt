package com.mbougar.swapware.data.repository

import com.mbougar.swapware.data.model.Ad
import com.mbougar.swapware.data.model.NewAdData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeAdRepository @Inject constructor() : AdRepository {
    private val adsFlow = MutableStateFlow<Result<List<Ad>>>(Result.success(emptyList()))

    fun setAds(ads: List<Ad>) {
        adsFlow.value = Result.success(ads)
    }

    override fun getAds(): Flow<Result<List<Ad>>> = adsFlow
    override fun getFavoriteAds(): Flow<List<Ad>> = flowOf(emptyList())
    override suspend fun addAd(adData: NewAdData): Result<Unit> = Result.success(Unit)
    override suspend fun updateAd(ad: Ad): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAd(ad: Ad): Result<Unit> = Result.success(Unit)
    override suspend fun toggleFavorite(adId: String, isFavorite: Boolean) {}
    override suspend fun refreshAds(): Result<Unit> = Result.success(Unit)
    override suspend fun getAdById(adId: String): Ad? = null
    override fun getAdsByUserId(userId: String): Flow<Result<List<Ad>>> = flowOf(Result.success(emptyList()))
    override suspend fun markAdAsSold(adId: String, buyerUserId: String): Result<Unit> = Result.success(Unit)
}
