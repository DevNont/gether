package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Meetup
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Firestore shape of trips/{tripId}/meetups/{meetupId}. date = ISO yyyy-MM-dd, time = HH:mm. */
data class MeetupDto(
    val title: String = "",
    val place: String? = null,
    val date: String = "",
    val time: String = "",
    val reminderMinutesBefore: Int = 30,
    val note: String? = null,
    val createdBy: String = "",
)

fun MeetupDto.toDomain(id: String): Meetup =
    Meetup(
        id = id,
        title = title,
        place = place,
        date = LocalDate.parse(date),
        time = LocalTime.parse(time),
        reminderMinutesBefore = reminderMinutesBefore,
        note = note,
        createdBy = createdBy,
    )

fun Meetup.toDto(): MeetupDto =
    MeetupDto(
        title = title,
        place = place,
        date = date.toString(),
        time = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}",
        reminderMinutesBefore = reminderMinutesBefore,
        note = note,
        createdBy = createdBy,
    )
