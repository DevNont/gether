package com.triptogether.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.triptogether.MainActivity
import com.triptogether.R

/** Fired by AlarmManager at a meetup's reminder time; shows a local notification. */
class MeetupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val tripId = intent.getStringExtra(Notifications.EXTRA_TRIP_ID) ?: return
        val meetupId = intent.getStringExtra(Notifications.EXTRA_MEETUP_ID) ?: return
        val title = intent.getStringExtra(Notifications.EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(Notifications.EXTRA_TEXT).orEmpty()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val tapIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(Notifications.EXTRA_TRIP_ID, tripId)
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                meetupId.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, Notifications.MEETUP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.meetup_notification_title, title))
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

        manager.notify(meetupId.hashCode(), notification)
    }
}
