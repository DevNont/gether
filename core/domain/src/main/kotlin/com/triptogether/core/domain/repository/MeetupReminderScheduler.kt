package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Meetup

/**
 * Schedules on-device reminders for a trip's meetups. Platform-agnostic so the domain stays free
 * of Android; the app module supplies the AlarmManager-backed implementation.
 */
interface MeetupReminderScheduler {
    /** (Re)schedule reminders for all [meetups] of a trip; past ones are skipped. */
    fun scheduleAll(
        tripId: String,
        tripName: String,
        meetups: List<Meetup>,
    )

    /** Cancel a scheduled reminder (e.g. when a meetup is deleted). */
    fun cancel(meetupId: String)
}
