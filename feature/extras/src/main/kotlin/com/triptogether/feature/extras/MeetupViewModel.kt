package com.triptogether.feature.extras

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Meetup
import com.triptogether.core.domain.repository.MeetupReminderScheduler
import com.triptogether.core.domain.repository.MeetupRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.extras.navigation.MeetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeetupUiState(
    val isLoading: Boolean = true,
    val meetups: List<Meetup> = emptyList(),
)

sealed interface MeetupEvent {
    data class Error(
        @StringRes val messageResId: Int,
    ) : MeetupEvent
}

@HiltViewModel
class MeetupViewModel
    @Inject
    constructor(
        private val meetupRepository: MeetupRepository,
        private val tripRepository: TripRepository,
        private val scheduler: MeetupReminderScheduler,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<MeetupRoute>()
        val tripId: String get() = route.tripId

        private val _events = Channel<MeetupEvent>(Channel.BUFFERED)
        val events: Flow<MeetupEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<MeetupUiState> =
            meetupRepository.observeMeetups(route.tripId)
                .map { MeetupUiState(isLoading = false, meetups = it) }
                .catch {
                    // A failed listener (e.g. PERMISSION_DENIED) must surface, not render as empty.
                    _events.send(MeetupEvent.Error(R.string.meetup_error))
                    emit(MeetupUiState(isLoading = false))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = MeetupUiState(),
                )

        init {
            // Schedule local reminders for whatever this device can see (creator + anyone who opens this).
            combine(
                meetupRepository.observeMeetups(route.tripId),
                tripRepository.observeTrip(route.tripId),
            ) { meetups, trip -> meetups to (trip?.name ?: "") }
                .onEach { (meetups, tripName) -> scheduler.scheduleAll(route.tripId, tripName, meetups) }
                // Reminders are best-effort: a failed listener stops scheduling but must not crash.
                .catch { }
                .launchIn(viewModelScope)
        }

        fun delete(meetup: Meetup) {
            viewModelScope.launch {
                meetupRepository.delete(route.tripId, meetup.id)
                    .onSuccess { scheduler.cancel(meetup.id) }
                    .onFailure { _events.send(MeetupEvent.Error(R.string.meetup_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
