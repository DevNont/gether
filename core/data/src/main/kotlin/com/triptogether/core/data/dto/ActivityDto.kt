package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.ActivityType
import com.triptogether.core.domain.model.Attachment
import kotlinx.datetime.LocalTime

/** Firestore shape of trips/{tripId}/days/{dayId}/activities/{activityId}. Times are HH:mm strings. */
data class ActivityDto(
    val title: String = "",
    val type: String = "PLACE",
    val startTime: String? = null,
    val endTime: String? = null,
    val placeName: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val note: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
    val sortOrder: Int = 0,
    val createdBy: String = "",
)

data class AttachmentDto(
    val name: String = "",
    val url: String = "",
    val mimeType: String = "",
)

fun ActivityDto.toDomain(id: String): Activity =
    Activity(
        id = id,
        title = title,
        type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.PLACE),
        // Times are optional; a corrupt value degrades to "no time" instead of crashing the listener.
        startTime = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        endTime = endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        placeName = placeName,
        lat = lat,
        lng = lng,
        note = note,
        attachments = attachments.map { Attachment(name = it.name, url = it.url, mimeType = it.mimeType) },
        sortOrder = sortOrder,
        createdBy = createdBy,
    )

fun Activity.toDto(): ActivityDto =
    ActivityDto(
        title = title,
        type = type.name,
        startTime = startTime?.toHHmm(),
        endTime = endTime?.toHHmm(),
        placeName = placeName,
        lat = lat,
        lng = lng,
        note = note,
        attachments = attachments.map { AttachmentDto(name = it.name, url = it.url, mimeType = it.mimeType) },
        sortOrder = sortOrder,
        createdBy = createdBy,
    )

/** Always two-digit HH:mm — LocalTime.toString() would append seconds when non-zero. */
fun LocalTime.toHHmm(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
