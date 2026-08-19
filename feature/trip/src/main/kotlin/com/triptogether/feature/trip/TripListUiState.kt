package com.triptogether.feature.trip

import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip

data class TripCardUi(
    val trip: Trip,
    val members: List<Member> = emptyList(),
    // Outstanding-balance badge arrives with the settlement logic in M5.
)

data class TripListUiState(
    val isLoading: Boolean = true,
    val upcoming: List<TripCardUi> = emptyList(),
    val past: List<TripCardUi> = emptyList(),
    /** Trips whose delete is in flight — the card shows a spinner until the listener drops it. */
    val deletingIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean get() = !isLoading && upcoming.isEmpty() && past.isEmpty()
}
