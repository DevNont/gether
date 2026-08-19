package com.triptogether.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.triptogether.core.domain.model.Meetup
import com.triptogether.core.domain.repository.MeetupReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules meetup reminders with AlarmManager. Uses an inexact alarm (setAndAllowWhileIdle) so it
 * needs no exact-alarm permission — a reminder that fires within a few minutes is fine and keeps the
 * app Play-policy-safe. Reminders live only on this device; full cross-device delivery needs FCM.
 */
@Singleton
class AndroidMeetupReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MeetupReminderScheduler {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)
        private val scheduledStore =
            context.getSharedPreferences("meetup_alarms", Context.MODE_PRIVATE)

        override fun scheduleAll(
            tripId: String,
            tripName: String,
            meetups: List<Meetup>,
        ) {
            val now = System.currentTimeMillis()
            // Take ownership of the whole set for this trip: cancel alarms whose
            // meetup was deleted elsewhere or has moved into the past.
            val storeKey = "trip_$tripId"
            val previous = scheduledStore.getStringSet(storeKey, emptySet()).orEmpty()
            val active =
                meetups.filter { triggerAtMillis(it) > now }.map { it.id }.toSet()
            (previous - active).forEach(::cancel)
            scheduledStore.edit().putStringSet(storeKey, active).apply()
            meetups.forEach { meetup ->
                val triggerAt = triggerAtMillis(meetup)
                if (triggerAt <= now) return@forEach
                val text =
                    buildString {
                        append(formatTime(meetup))
                        meetup.place?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    }
                val intent =
                    reminderIntent(meetup.id).apply {
                        putExtra(Notifications.EXTRA_TRIP_ID, tripId)
                        putExtra(Notifications.EXTRA_MEETUP_ID, meetup.id)
                        putExtra(Notifications.EXTRA_TITLE, meetup.title)
                        putExtra(Notifications.EXTRA_TEXT, text)
                    }
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(meetup.id, intent))
            }
        }

        override fun cancel(meetupId: String) {
            alarmManager.cancel(pendingIntent(meetupId, reminderIntent(meetupId)))
        }

        private fun triggerAtMillis(meetup: Meetup): Long {
            val at = meetup.date.atTime(meetup.time).toInstant(TimeZone.currentSystemDefault())
            return at.toEpochMilliseconds() - meetup.reminderMinutesBefore * MILLIS_PER_MINUTE
        }

        private fun reminderIntent(meetupId: String): Intent =
            Intent(context, MeetupReminderReceiver::class.java).setData(
                android.net.Uri.parse("triptogether://meetup/$meetupId"),
            )

        private fun pendingIntent(
            meetupId: String,
            intent: Intent,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                meetupId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun formatTime(meetup: Meetup): String =
            "${meetup.time.hour.toString().padStart(2, '0')}:${meetup.time.minute.toString().padStart(2, '0')}"

        private companion object {
            const val MILLIS_PER_MINUTE = 60_000L
        }
    }
