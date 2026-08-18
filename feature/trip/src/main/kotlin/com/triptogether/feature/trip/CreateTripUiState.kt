package com.triptogether.feature.trip

import androidx.annotation.StringRes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

const val MAX_TRIP_DAYS = 60

data class CreateTripUiState(
    val name: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isSaving: Boolean = false,
    val isExisting: Boolean = false,
    val isLoading: Boolean = false,
    /** Editing shrank the date range — confirm that dropped days lose their activities. */
    val showShrinkConfirm: Boolean = false,
) {
    val dayCount: Int?
        get() =
            if (startDate != null && endDate != null) startDate.daysUntil(endDate) + 1 else null

    val isRangeTooLong: Boolean get() = (dayCount ?: 0) > MAX_TRIP_DAYS

    val canSave: Boolean
        get() = name.isNotBlank() && startDate != null && endDate != null && !isRangeTooLong && !isSaving
}

sealed interface CreateTripEvent {
    data class Created(val tripId: String) : CreateTripEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : CreateTripEvent
}
