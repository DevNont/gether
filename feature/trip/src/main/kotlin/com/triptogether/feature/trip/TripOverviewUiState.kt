package com.triptogether.feature.trip

import androidx.annotation.StringRes
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip

data class TripOverviewUiState(
    val isLoading: Boolean = true,
    /** The trip document no longer exists (e.g. deleted by the owner while this screen was open). */
    val notFound: Boolean = false,
    val trip: Trip? = null,
    val members: List<Member> = emptyList(),
    val isOwner: Boolean = false,
    val noteDraft: String = "",
    val isSavingNote: Boolean = false,
) {
    val noteChanged: Boolean get() = trip != null && noteDraft != (trip.note ?: "")
}

sealed interface TripOverviewEvent {
    data class Message(
        @StringRes val messageResId: Int,
    ) : TripOverviewEvent

    data object Deleted : TripOverviewEvent
}
