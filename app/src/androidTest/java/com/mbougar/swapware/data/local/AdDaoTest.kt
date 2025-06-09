package com.mbougar.swapware.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.mbougar.swapware.data.model.Ad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AdDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var adDao: AdDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        adDao = database.adDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAdAndGetById() = runBlocking {
        val ad = Ad(id = "1", title = "Test Ad")
        adDao.insertAd(ad)

        val loaded = adDao.getAdById(ad.id)
        assertThat(loaded).isNotNull()
        assertThat(loaded?.id).isEqualTo(ad.id)
        assertThat(loaded?.title).isEqualTo(ad.title)
    }

    @Test
    fun getAllAdsReturnsAllAdsFromDb() = runBlocking {
        val ad1 = Ad(id = "1", title = "Ad 1")
        val ad2 = Ad(id = "2", title = "Ad 2")
        adDao.insertAds(listOf(ad1, ad2))

        val allAds = adDao.getAllAds().first()
        assertThat(allAds).hasSize(2)
        assertThat(allAds).containsExactly(ad1, ad2).inOrder()
    }

    @Test
    fun getFavoriteAdsReturnsOnlyFavorites() = runBlocking {
        val favoriteAd = Ad(id = "1", title = "Favorite Ad", isFavorite = true)
        val normalAd = Ad(id = "2", title = "Normal Ad", isFavorite = false)
        adDao.insertAds(listOf(favoriteAd, normalAd))

        val favoriteAds = adDao.getFavoriteAds().first()
        assertThat(favoriteAds).hasSize(1)
        assertThat(favoriteAds.first()).isEqualTo(favoriteAd)
    }

    @Test
    fun updateAdReflectsChangesInDb() = runBlocking {
        val ad = Ad(id = "1", title = "Original Title")
        adDao.insertAd(ad)

        val updatedAd = ad.copy(title = "Updated Title")
        adDao.updateAd(updatedAd)

        val loaded = adDao.getAdById("1")
        assertThat(loaded?.title).isEqualTo("Updated Title")
    }

    @Test
    fun deleteAdByIdRemovesFromDb() = runBlocking {
        val ad = Ad(id = "1", title = "To Be Deleted")
        adDao.insertAd(ad)

        adDao.deleteAdById("1")

        val loaded = adDao.getAdById("1")
        assertThat(loaded).isNull()
    }

    @Test
    fun deleteAllAdsClearsTheTable() = runBlocking {
        val ad1 = Ad(id = "1", title = "Ad 1")
        val ad2 = Ad(id = "2", title = "Ad 2")
        adDao.insertAds(listOf(ad1, ad2))

        adDao.deleteAllAds()

        val allAds = adDao.getAllAds().first()
        assertThat(allAds).isEmpty()
    }
}
