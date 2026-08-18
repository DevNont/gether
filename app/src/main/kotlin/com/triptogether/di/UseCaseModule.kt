package com.triptogether.di

import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.core.domain.usecase.GetBalancesUseCase
import com.triptogether.core.domain.usecase.GetSuggestedTransfersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Use cases are plain classes — core:domain stays free of DI annotations for the KMP move. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideGetBalancesUseCase(
        tripRepository: TripRepository,
        expenseRepository: ExpenseRepository,
        settlementRepository: SettlementRepository,
    ): GetBalancesUseCase = GetBalancesUseCase(tripRepository, expenseRepository, settlementRepository)

    @Provides
    @Singleton
    fun provideGetSuggestedTransfersUseCase(getBalances: GetBalancesUseCase): GetSuggestedTransfersUseCase =
        GetSuggestedTransfersUseCase(getBalances)
}
