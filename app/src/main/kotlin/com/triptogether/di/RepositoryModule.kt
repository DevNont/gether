package com.triptogether.di

import com.triptogether.core.data.repository.FirebaseAuthRepository
import com.triptogether.core.data.repository.FirestoreUserRepository
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The app module is the only place where core:data implementations bind to core:domain interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: FirestoreUserRepository): UserRepository
}
