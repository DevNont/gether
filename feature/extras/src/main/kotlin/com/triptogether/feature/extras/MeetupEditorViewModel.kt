package com.triptogether.feature.extras

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Meetup
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.MeetupReminderScheduler
import com.triptogether.core.domain.repository.MeetupRepository
import com.triptogether.feature.extras.navigation.MeetupEditorRoute
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
import kotlinx.datetime.LocalTime
import javax.inject.Inject

data class MeetupEditorUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val place: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val reminderMinutesBefore: Int = 30,
    val note: String = "",
    val isExisting: Boolean = false,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && date != null && time != null && !isSaving
}

sealed interface MeetupEditorEvent {
    data object Saved : MeetupEditorEvent

    data object Deleted : MeetupEditorEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : MeetupEditorEvent
}

@HiltViewModel
class MeetupEditorViewModel
    @Inject
    constructor(
        private val meetupRepository: MeetupRepository,
        private val authRepository: AuthRepository,
        private val reminderScheduler: MeetupReminderScheduler,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<MeetupEditorRoute>()

        private val _uiState = MutableStateFlow(MeetupEditorUiState())
        val uiState: StateFlow<MeetupEditorUiState> = _uiState.asStateFlow()

        private val _events = Channel<MeetupEditorEvent>(Channel.BUFFERED)
        val events: Flow<MeetupEditorEvent> = _events.receiveAsFlow()

        private var existing: Meetup? = null

        init {
            route.meetupId?.let { load(it) }
        }

        private fun load(meetupId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                // A failed listener (e.g. PERMISSION_DENIED) must not crash the scope or spin forever.
                val meetup =
                    runCatching { meetupRepository.observeMeetups(route.tripId).first() }
                        .getOrNull()?.firstOrNull { it.id == meetupId }
                existing = meetup
                _uiState.update {
                    if (meetup == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            isLoading = false,
                            title = meetup.title,
                            place = meetup.place.orEmpty(),
                            date = meetup.date,
                            time = meetup.time,
                            reminderMinutesBefore = meetup.reminderMinutesBefore,
                            note = meetup.note.orEmpty(),
                            isExisting = true,
                        )
                    }
                }
            }
        }

        fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

        fun onPlaceChange(value: String) = _uiState.update { it.copy(place = value) }

        fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }

        fun onTimeChange(value: LocalTime) = _uiState.update { it.copy(time = value) }

        fun onReminderChange(value: Int) = _uiState.update { it.copy(reminderMinutesBefore = value) }

        fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

        fun save() {
            val state = _uiState.value
            if (!state.canSave) return
            val date = state.date ?: return
            val time = state.time ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val createdBy = existing?.createdBy ?: authRepository.observeAuthState().filterNotNull().first().id
                val meetup =
                    Meetup(
                        id = existing?.id ?: "",
                        title = state.title.trim(),
                        place = state.place.trim().ifBlank { null },
                        date = date,
                        time = time,
                        reminderMinutesBefore = state.reminderMinutesBefore,
                        note = state.note.trim().ifBlank { null },
                        createdBy = createdBy,
                    )
                meetupRepository.upsert(route.tripId, meetup)
                    .onSuccess { _events.send(MeetupEditorEvent.Saved) }
                    .onFailure { _events.send(MeetupEditorEvent.Error(R.string.meetup_error)) }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        fun delete() {
            val id = existing?.id ?: return
            viewModelScope.launch {
                meetupRepository.delete(route.tripId, id)
                    .onSuccess {
                        // The alarm must die with the meetup, or a ghost reminder fires.
                        reminderScheduler.cancel(id)
                        _events.send(MeetupEditorEvent.Deleted)
                    }
                    .onFailure { _events.send(MeetupEditorEvent.Error(R.string.meetup_error)) }
            }
        }
    }
