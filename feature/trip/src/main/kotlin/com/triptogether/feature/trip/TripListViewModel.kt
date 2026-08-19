package com.triptogether.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.DemoSeeder
import com.triptogether.core.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripListViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val tripRepository: TripRepository,
        private val demoSeeder: DemoSeeder,
    ) : ViewModel() {
        private val _seedError = Channel<Unit>(Channel.BUFFERED)
        val seedError: Flow<Unit> = _seedError.receiveAsFlow()

        private val _deleteError = Channel<Unit>(Channel.BUFFERED)
        val deleteError: Flow<Unit> = _deleteError.receiveAsFlow()

        private val deletingIds = MutableStateFlow<Set<String>>(emptySet())

        /** Debug builds only — populate a demo trip to browse every screen. */
        fun seedDemoTrip() {
            viewModelScope.launch {
                demoSeeder.seedJapanTrip().onFailure { _seedError.send(Unit) }
            }
        }

        /** Delete a trip from its card; the card shows a spinner until the listener drops it. */
        fun deleteTrip(tripId: String) {
            deletingIds.update { it + tripId }
            viewModelScope.launch {
                tripRepository.deleteTrip(tripId).onFailure { _deleteError.send(Unit) }
                deletingIds.update { it - tripId }
            }
        }

        val uiState: StateFlow<TripListUiState> =
            combine(
                authRepository.observeAuthState()
                    .filterNotNull()
                    .flatMapLatest { user -> tripRepository.observeTrips(user.id) }
                    .flatMapLatest { trips -> withMembers(trips) },
                deletingIds,
            ) { cards, deleting -> buildState(cards, deleting) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = TripListUiState(isLoading = true),
                )

        private fun withMembers(trips: List<Trip>) =
            if (trips.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    trips.map { trip ->
                        tripRepository.observeMembers(trip.id).map { TripCardUi(trip = trip, members = it) }
                    },
                ) { it.toList() }
            }

        private fun buildState(
            cards: List<TripCardUi>,
            deleting: Set<String>,
        ): TripListUiState {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            // Archived trips stay out of the list until trip settings (S14) exposes them.
            val visible = cards.filterNot { it.trip.archived }
            val (upcoming, past) = visible.partition { it.trip.endDate >= today }
            return TripListUiState(
                isLoading = false,
                upcoming = upcoming.sortedByDescending { it.trip.startDate },
                past = past.sortedByDescending { it.trip.endDate },
                deletingIds = deleting,
            )
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
