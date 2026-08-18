package com.triptogether.feature.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.trip.navigation.CreateTripRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** Creates a new trip or edits an existing one (route.tripId != null). */
@HiltViewModel
class CreateTripViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        private val analytics: AnalyticsLogger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<CreateTripRoute>()

        private val _uiState = MutableStateFlow(CreateTripUiState(isLoading = route.tripId != null))
        val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

        private val _events = Channel<CreateTripEvent>(Channel.BUFFERED)
        val events: Flow<CreateTripEvent> = _events.receiveAsFlow()

        private var existing: Trip? = null

        init {
            route.tripId?.let { tripId ->
                viewModelScope.launch {
                    val trip = tripRepository.observeTrip(tripId).filterNotNull().first()
                    existing = trip
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isExisting = true,
                            name = trip.name,
                            startDate = trip.startDate,
                            endDate = trip.endDate,
                        )
                    }
                }
            }
        }

        fun onNameChange(name: String) {
            _uiState.update { it.copy(name = name) }
        }

        fun onDatesSelected(
            start: LocalDate,
            end: LocalDate,
        ) {
            _uiState.update { it.copy(startDate = start, endDate = end) }
        }

        fun dismissShrinkConfirm() {
            _uiState.update { it.copy(showShrinkConfirm = false) }
        }

        fun save() {
            val state = _uiState.value
            val start = state.startDate ?: return
            val end = state.endDate ?: return
            if (!state.canSave) return

            val old = existing
            if (old != null && rangeShrunk(old, start, end) && !state.showShrinkConfirm) {
                // S05: shrinking the range deletes those days' activities — confirm first.
                _uiState.update { it.copy(showShrinkConfirm = true) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, showShrinkConfirm = false) }
                val result =
                    if (old == null) {
                        tripRepository.createTrip(
                            TripDraft(name = state.name.trim(), startDate = start, endDate = end),
                        ).map { tripId ->
                            analytics.log(AnalyticsLogger.TRIP_CREATED)
                            tripId
                        }
                    } else {
                        tripRepository.updateTrip(
                            old.copy(name = state.name.trim(), startDate = start, endDate = end),
                        ).map { old.id }
                    }
                result
                    .onSuccess { tripId -> _events.send(CreateTripEvent.Created(tripId)) }
                    .onFailure { _events.send(CreateTripEvent.Error(R.string.create_trip_error)) }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private fun rangeShrunk(
            old: Trip,
            newStart: LocalDate,
            newEnd: LocalDate,
        ): Boolean = newStart > old.startDate || newEnd < old.endDate
    }
