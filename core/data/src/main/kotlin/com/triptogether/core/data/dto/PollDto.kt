package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Poll
import com.triptogether.core.domain.model.PollOption

/** Firestore shape of trips/{tripId}/polls/{pollId}. Options embed their voters. */
data class PollDto(
    val question: String = "",
    val options: List<PollOptionDto> = emptyList(),
    val multiChoice: Boolean = false,
    val closed: Boolean = false,
    val activityId: String? = null,
    val createdBy: String = "",
)

data class PollOptionDto(
    val id: String = "",
    val label: String = "",
    val voterMemberIds: List<String> = emptyList(),
)

fun PollDto.toDomain(id: String): Poll =
    Poll(
        id = id,
        question = question,
        options =
            options.map {
                PollOption(id = it.id, label = it.label, voterMemberIds = it.voterMemberIds.toSet())
            },
        multiChoice = multiChoice,
        closed = closed,
        activityId = activityId,
        createdBy = createdBy,
    )

fun Poll.toDto(): PollDto =
    PollDto(
        question = question,
        options =
            options.map {
                PollOptionDto(id = it.id, label = it.label, voterMemberIds = it.voterMemberIds.toList())
            },
        multiChoice = multiChoice,
        closed = closed,
        activityId = activityId,
        createdBy = createdBy,
    )
