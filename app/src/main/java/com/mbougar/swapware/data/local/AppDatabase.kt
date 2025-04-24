package com.mbougar.swapware.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mbougar.swapware.data.model.Ad

@Database(entities = [Ad::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adDao(): AdDao
}