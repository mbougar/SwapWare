package com.mbougar.swapware.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PoblacionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var poblacionDao: PoblacionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        poblacionDao = database.poblacionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetPoblacionByNombreAndProvincia() = runBlocking {
        val poblacion = PoblacionLocation("Madrid", "Madrid", 40.4168, -3.7038)
        poblacionDao.insert(poblacion)

        val loaded = poblacionDao.getPoblacionByNombreAndProvincia("Madrid", "Madrid")
        assertThat(loaded).isNotNull()
        assertThat(loaded).isEqualTo(poblacion)
    }

    @Test
    fun getCountReturnsCorrectNumberOfRows() = runBlocking {
        val p1 = PoblacionLocation("Madrid", "Madrid", 0.0, 0.0)
        val p2 = PoblacionLocation("Barcelona", "Barcelona", 0.0, 0.0)
        poblacionDao.insertAll(listOf(p1, p2))

        val count = poblacionDao.getCount()
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun searchPoblacionesReturnsMatchingResults() = runBlocking {
        val p1 = PoblacionLocation("Madrid", "Madrid", 0.0, 0.0)
        val p2 = PoblacionLocation("Majadahonda", "Madrid", 0.0, 0.0)
        val p3 = PoblacionLocation("Barcelona", "Barcelona", 0.0, 0.0)
        poblacionDao.insertAll(listOf(p1, p2, p3))

        val results = poblacionDao.searchPoblaciones("Maja%")
        assertThat(results).hasSize(1)
        assertThat(results.first().poblacionName).isEqualTo("Majadahonda")
    }

    @Test
    fun searchPoblacionesIsCaseInsensitiveAndRespectsLimit() = runBlocking {
        val p1 = PoblacionLocation("Madrid", "Madrid", 0.0, 0.0)
        val p2 = PoblacionLocation("Majadahonda", "Madrid", 0.0, 0.0)
        poblacionDao.insertAll(listOf(p1, p2))

        val results = poblacionDao.searchPoblaciones("mad%", limit = 1)
        assertThat(results).hasSize(1)
    }

    @Test
    fun onConflictReplaceStrategyWorks() = runBlocking {
        val p1 = PoblacionLocation("Madrid", "Madrid", 40.0, -3.0)
        poblacionDao.insert(p1)

        val p1Updated = PoblacionLocation("Madrid", "Madrid", 40.4168, -3.7038)
        poblacionDao.insert(p1Updated)

        val loaded = poblacionDao.getPoblacionByNombreAndProvincia("Madrid", "Madrid")
        assertThat(loaded?.latitude).isEqualTo(40.4168)
        assertThat(poblacionDao.getCount()).isEqualTo(1)
    }
}
