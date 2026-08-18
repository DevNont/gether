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
)

fun TripDto.toDomain(id: String): Trip =
    Trip(
        id = id,
        name = name,
        coverUrl = coverUrl,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        ownerId = ownerId,
        inviteCode = inviteCode,
        archived = archived,
    )
