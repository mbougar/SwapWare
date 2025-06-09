package com.mbougar.swapware.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PoblacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poblaciones: List<PoblacionLocation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poblacion: PoblacionLocation)

    @Query("SELECT * FROM poblaciones WHERE poblacionName LIKE :query OR provincia LIKE :query ORDER BY poblacionName ASC LIMIT :limit")
    suspend fun searchPoblaciones(query: String, limit: Int = 20): List<PoblacionLocation>

    @Query("SELECT COUNT(*) FROM poblaciones")
    suspend fun getCount(): Int

    @Query("SELECT * FROM poblaciones WHERE poblacionName = :poblacionName AND provincia = :provincia LIMIT 1")
    suspend fun getPoblacionByNombreAndProvincia(poblacionName: String, provincia: String): PoblacionLocation?
}