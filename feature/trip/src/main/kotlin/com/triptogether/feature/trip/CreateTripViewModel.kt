package com.triptogether.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class CreateTripViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        private val analytics: AnalyticsLogger,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CreateTripUiState())
        val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

        private val _events = Channel<CreateTripEvent>(Channel.BUFFERED)
        val events: Flow<CreateTripEvent> = _events.receiveAsFlow()

        fun onNameChange(name: String) {
            _uiState.update { it.copy(name = name) }
        }

        fun onDatesSelected(
            start: LocalDate,
            end: LocalDate,
        ) {
            _uiState.update { it.copy(startDate = start, endDate = end) }
        }

        fun createTrip() {
            val state = _uiState.value
            val start = state.startDate ?: return
            val end = state.endDate ?: return
            if (!state.canSave) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                tripRepository.createTrip(
                    TripDraft(name = state.name.trim(), startDate = start, endDate = end),
                )
                    .onSuccess { tripId ->
                        analytics.log(AnalyticsLogger.TRIP_CREATED)
                        _events.send(CreateTripEvent.Created(tripId))
                    }
                    .onFailure { _events.send(CreateTripEvent.Error(R.string.create_trip_error)) }
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
