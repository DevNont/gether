package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Trip
import kotlinx.datetime.LocalDate

/** Firestore shape of trips/{tripId}. Dates are yyyy-MM-dd strings to avoid timezone issues. */
data class TripDto(
    val name: String = "",
    val coverUrl: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val currency: String = "THB",
    val inviteCode: String = "",
    val archived: Boolean = false,
    val note: String? = null,
)

/** Returns null when the stored dates are corrupt — a bad doc must be skipped, not crash a listener. */
fun TripDto.toDomain(id: String): Trip? {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return null
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return null
    return Trip(
        id = id,
        name = name,
        coverUrl = coverUrl,
        startDate = start,
        endDate = end,
        ownerId = ownerId,
        inviteCode = inviteCode,
        archived = archived,
        note = note,
    )
}
