package com.triptogether.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.MeetupReminderScheduler
import com.triptogether.core.domain.repository.MeetupRepository
import com.triptogether.core.domain.repository.TripRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Re-registers every pending meetup alarm after events that wipe or invalidate them:
 * reboot, app update, and timezone changes (alarms are absolute epoch millis, so a
 * timezone move shifts every local meetup time). Reads trips + meetups from the
 * Firestore offline cache, so this works without a network round-trip.
 */
class MeetupRescheduleReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun authRepository(): AuthRepository

        fun tripRepository(): TripRepository

        fun meetupRepository(): MeetupRepository

        fun scheduler(): MeetupReminderScheduler
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    val user = deps.authRepository().observeAuthState().firstOrNull() ?: return@withTimeoutOrNull
                    val trips = deps.tripRepository().observeTrips(user.id).first()
                    trips.forEach { trip ->
                        val meetups = deps.meetupRepository().observeMeetups(trip.id).first()
                        deps.scheduler().scheduleAll(trip.id, trip.name, meetups)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        const val TIMEOUT_MS = 8_000L
    }
}
