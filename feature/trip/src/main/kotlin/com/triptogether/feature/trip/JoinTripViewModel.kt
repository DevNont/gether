package com.triptogether.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.model.INVITE_CODE_LENGTH
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.NetworkMonitor
import com.triptogether.core.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import javax.inject.Inject

@HiltViewModel
class JoinTripViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        private val authRepository: AuthRepository,
        private val analytics: AnalyticsLogger,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(JoinTripUiState())
        val uiState: StateFlow<JoinTripUiState> = _uiState.asStateFlow()

        private val _events = Channel<JoinTripEvent>(Channel.BUFFERED)
        val events: Flow<JoinTripEvent> = _events.receiveAsFlow()

        private var lookupJob: Job? = null

        fun onCodeChange(raw: String) {
            val code = raw.filter { it.isDigit() }.take(INVITE_CODE_LENGTH)
            _uiState.update {
                it.copy(code = code, preview = null, notFound = false)
            }
            lookupJob?.cancel()
            if (code.length == INVITE_CODE_LENGTH) {
                lookupJob = lookup(code)
            }
        }

        private fun lookup(code: String): Job =
            viewModelScope.launch {
                _uiState.update { it.copy(isLookingUp = true) }
                tripRepository.getInvitePreview(code)
                    .onSuccess { preview ->
                        _uiState.update { it.copy(isLookingUp = false, preview = preview, notFound = false) }
                    }
                    .onFailure {
                        // Offline lookups always fail — say so instead of a misleading "code not found".
                        if (networkMonitor.isOnline.first()) {
                            _uiState.update { it.copy(isLookingUp = false, preview = null, notFound = true) }
                        } else {
                            _uiState.update { it.copy(isLookingUp = false, preview = null, notFound = false) }
                            _events.send(JoinTripEvent.Error(R.string.join_trip_offline))
                        }
                    }
            }

        fun confirmJoin() {
            val state = _uiState.value
            if (!state.canConfirm) return
            viewModelScope.launch {
                _uiState.update { it.copy(isJoining = true) }
                val userId = authRepository.observeAuthState().filterNotNull().first().id
                tripRepository.joinByCode(state.code, userId)
                    .onSuccess { tripId ->
                        analytics.log(AnalyticsLogger.TRIP_JOINED)
                        _events.send(JoinTripEvent.Joined(tripId))
                    }
                    .onFailure { _events.send(JoinTripEvent.Error(R.string.join_trip_error)) }
                _uiState.update { it.copy(isJoining = false) }
            }
        }
    }
