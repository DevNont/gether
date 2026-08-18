package com.triptogether.feature.plan

import androidx.annotation.StringRes
import kotlinx.datetime.LocalTime

data class ActivityEditorUiState(
    val title: String = "",
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val placeName: String = "",
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
