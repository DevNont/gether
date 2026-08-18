package com.triptogether.feature.trip

import androidx.annotation.StringRes
import com.triptogether.core.domain.model.InvitePreview

data class JoinTripUiState(
    val code: String = "",
    val preview: InvitePreview? = null,
    val isLookingUp: Boolean = false,
    val notFound: Boolean = false,
    val isJoining: Boolean = false,
) {
    val canConfirm: Boolean get() = preview != null && !isJoining
}

sealed interface JoinTripEvent {
    data class Joined(val tripId: String) : JoinTripEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : JoinTripEvent
}
