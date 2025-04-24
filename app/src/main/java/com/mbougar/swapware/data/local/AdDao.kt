package com.mbougar.swapware.data.local

import androidx.room.*
import com.mbougar.swapware.data.model.Ad
import kotlinx.coroutines.flow.Flow

@Dao
interface AdDao {
    @Query("SELECT * FROM ads ORDER BY timestamp DESC")
    fun getAllAds(): Flow<List<Ad>>

    @Query("SELECT * FROM ads WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteAds(): Flow<List<Ad>>

    @Query("SELECT * FROM ads WHERE id = :adId")
    suspend fun getAdById(adId: String): Ad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAds(ads: List<Ad>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAd(ad: Ad)

    @Update
    suspend fun updateAd(ad: Ad)

    @Query("DELETE FROM ads WHERE id = :adId")
    suspend fun deleteAdById(adId: String)

    @Query("DELETE FROM ads")
    suspend fun deleteAllAds()
}