package com.triptogether.feature.extras

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeetupUiState(
    val isLoading: Boolean = true,
    val meetups: List<Meetup> = emptyList(),
)

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

        val uiState: StateFlow<MeetupUiState> =
            meetupRepository.observeMeetups(route.tripId)
                .map { MeetupUiState(isLoading = false, meetups = it) }
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
                .launchIn(viewModelScope)
        }

        fun delete(meetup: Meetup) {
            viewModelScope.launch {
                meetupRepository.delete(route.tripId, meetup.id)
                scheduler.cancel(meetup.id)
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
