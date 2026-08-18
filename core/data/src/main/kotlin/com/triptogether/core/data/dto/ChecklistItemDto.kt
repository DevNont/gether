package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.ChecklistItem
import com.triptogether.core.domain.model.ChecklistScope

/** Firestore shape of trips/{tripId}/checklist/{itemId}. */
data class ChecklistItemDto(
    val title: String = "",
    val scope: String = "SHARED",
    val assignedMemberId: String? = null,
    val checkedBy: List<String> = emptyList(),
    val sortOrder: Int = 0,
)

fun ChecklistItemDto.toDomain(id: String): ChecklistItem =
    ChecklistItem(
        id = id,
        title = title,
        scope = runCatching { ChecklistScope.valueOf(scope) }.getOrDefault(ChecklistScope.SHARED),
        assignedMemberId = assignedMemberId,
        checkedBy = checkedBy.toSet(),
        sortOrder = sortOrder,
    )

fun ChecklistItem.toDto(): ChecklistItemDto =
    ChecklistItemDto(
        title = title,
        scope = scope.name,
        assignedMemberId = assignedMemberId,
        checkedBy = checkedBy.toList(),
        sortOrder = sortOrder,
    )
