package com.mbougar.swapware.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mbougar.swapware.data.model.Ad

@Database(entities = [Ad::class, PoblacionLocation::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adDao(): AdDao
    abstract fun poblacionDao(): PoblacionDao
}