package com.triptogether.feature.plan

import androidx.annotation.StringRes
import com.triptogether.core.domain.model.ActivityType
import kotlinx.datetime.LocalTime

data class ActivityEditorUiState(
    val title: String = "",
    val type: ActivityType = ActivityType.PLACE,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val placeName: String = "",
    /** Coordinates from a Places pick; null when the place was only typed. */
    val lat: Double? = null,
    val lng: Double? = null,
    val note: String = "",
    val isExisting: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isTimeRangeInvalid: Boolean
        get() = startTime != null && endTime != null && endTime < startTime

    val canSave: Boolean get() = title.isNotBlank() && !isTimeRangeInvalid && !isSaving
}

sealed interface ActivityEditorEvent {
    data object Saved : ActivityEditorEvent

    data object Deleted : ActivityEditorEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : ActivityEditorEvent
}
