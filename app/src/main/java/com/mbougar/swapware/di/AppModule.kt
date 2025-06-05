package com.mbougar.swapware.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mbougar.swapware.data.local.AdDao
import com.mbougar.swapware.data.local.AppDatabase
import com.mbougar.swapware.data.local.DatabasePopulator
import com.mbougar.swapware.data.local.PoblacionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        populatorProvider: Provider<DatabasePopulator>
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "marketplace_db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    populatorProvider.get().populateOnCreate()
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun providePoblacionDao(database: AppDatabase): PoblacionDao = database.poblacionDao()

    @Provides
    @Singleton
    fun provideAdDao(database: AppDatabase): AdDao = database.adDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideIoCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO)
}