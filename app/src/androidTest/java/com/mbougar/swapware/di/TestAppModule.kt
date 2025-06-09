package com.mbougar.swapware.di

import androidx.test.espresso.core.internal.deps.dagger.Provides
import com.mbougar.swapware.data.local.PoblacionDao
import com.mbougar.swapware.data.repository.AdRepository
import com.mbougar.swapware.data.repository.AuthRepository
import com.mbougar.swapware.data.repository.FakeAdRepository
import com.mbougar.swapware.data.repository.FakeAuthRepository
import com.mbougar.swapware.data.repository.FakeMessageRepository
import com.mbougar.swapware.data.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestAppModule {

    @Binds
    @Singleton
    abstract fun bindFakeAuthRepository(
        fakeAuthRepository: FakeAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFakeAdRepository(
        fakeAdRepository: FakeAdRepository
    ): AdRepository

    @Binds
    @Singleton
    abstract fun bindFakeMessageRepository(
        fakeMessageRepository: FakeMessageRepository
    ): MessageRepository

    companion object {
        @Provides
        @Singleton
        fun providePoblacionDao(): PoblacionDao {
            return mockk(relaxed = true)
        }
    }
}
