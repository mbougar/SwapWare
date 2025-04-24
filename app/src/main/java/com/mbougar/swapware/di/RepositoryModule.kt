package com.mbougar.swapware.di

import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AdRepositoryImpl
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.AuthRepositoryImpl
import com.mbougar.swapware.data.repository.MessageRepository
import com.mbougar.swapware.data.repository.MessageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdRepository(impl: AdRepositoryImpl): AdRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
}