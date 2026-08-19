package com.triptogether.di

import com.triptogether.core.data.analytics.FirebaseAnalyticsLogger
import com.triptogether.core.data.demo.DefaultDemoSeeder
import com.triptogether.core.data.network.ConnectivityNetworkMonitor
import com.triptogether.core.data.repository.FirebaseAuthRepository
import com.triptogether.core.data.repository.FirestoreChecklistRepository
import com.triptogether.core.data.repository.FirestoreExpenseRepository
import com.triptogether.core.data.repository.FirestoreMeetupRepository
import com.triptogether.core.data.repository.FirestorePlanRepository
import com.triptogether.core.data.repository.FirestorePollRepository
import com.triptogether.core.data.repository.FirestoreSettlementRepository
import com.triptogether.core.data.repository.FirestoreTripRepository
import com.triptogether.core.data.repository.FirestoreUserRepository
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ChecklistRepository
import com.triptogether.core.domain.repository.DemoSeeder
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.MeetupReminderScheduler
import com.triptogether.core.domain.repository.MeetupRepository
import com.triptogether.core.domain.repository.NetworkMonitor
import com.triptogether.core.domain.repository.PlanRepository
import com.triptogether.core.domain.repository.PollRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.core.domain.repository.UserRepository
import com.triptogether.notifications.AndroidMeetupReminderScheduler
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

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: FirestoreTripRepository): TripRepository

    @Binds
    @Singleton
    abstract fun bindPlanRepository(impl: FirestorePlanRepository): PlanRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: FirestoreExpenseRepository): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSettlementRepository(impl: FirestoreSettlementRepository): SettlementRepository

    @Binds
    @Singleton
    abstract fun bindChecklistRepository(impl: FirestoreChecklistRepository): ChecklistRepository

    @Binds
    @Singleton
    abstract fun bindMeetupRepository(impl: FirestoreMeetupRepository): MeetupRepository

    @Binds
    @Singleton
    abstract fun bindMeetupReminderScheduler(impl: AndroidMeetupReminderScheduler): MeetupReminderScheduler

    @Binds
    @Singleton
    abstract fun bindPollRepository(impl: FirestorePollRepository): PollRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindAnalyticsLogger(impl: FirebaseAnalyticsLogger): AnalyticsLogger

    @Binds
    @Singleton
    abstract fun bindDemoSeeder(impl: DefaultDemoSeeder): DemoSeeder
}
