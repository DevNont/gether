package com.triptogether.feature.trip

import androidx.annotation.StringRes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

const val MAX_TRIP_DAYS = 60

/** A date range already taken by another of the user's trips. */
data class BusyRange(
    val tripName: String,
    val start: LocalDate,
    val end: LocalDate,
)

data class CreateTripUiState(
    val name: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isSaving: Boolean = false,
    val isExisting: Boolean = false,
    val isLoading: Boolean = false,
    /** Editing shrank the date range — confirm that dropped days lose their activities. */
    val showShrinkConfirm: Boolean = false,
    /** Set by the ViewModel: dates in the past (create only) or overlapping another trip. */
    @StringRes val dateErrorResId: Int? = null,
    /** Name of the trip the selected range collides with, for the error message. */
    val dateErrorArg: String = "",
    /** Every day already booked by another trip — greyed out and unselectable in the picker. */
    val busyDates: Set<LocalDate> = emptySet(),
    /** The busy ranges as spans, for the legend under the date field. */
    val busyRanges: List<BusyRange> = emptyList(),
) {
    val dayCount: Int?
        get() =
            if (startDate != null && endDate != null) startDate.daysUntil(endDate) + 1 else null

    val isRangeTooLong: Boolean get() = (dayCount ?: 0) > MAX_TRIP_DAYS

    val canSave: Boolean
        get() =
            name.isNotBlank() && startDate != null && endDate != null &&
                !isRangeTooLong && dateErrorResId == null && !isSaving
}

sealed interface CreateTripEvent {
    data class Created(val tripId: String) : CreateTripEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : CreateTripEvent
}
