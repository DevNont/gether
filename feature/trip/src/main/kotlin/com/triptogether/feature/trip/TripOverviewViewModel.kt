package com.triptogether.feature.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.trip.navigation.TripOverviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripOverviewViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<TripOverviewRoute>()

        private val noteState = MutableStateFlow(NoteState())

        private data class NoteState(
            val draft: String = "",
            val touched: Boolean = false,
            val saving: Boolean = false,
        )

        private val _events = Channel<TripOverviewEvent>(Channel.BUFFERED)
        val events: Flow<TripOverviewEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<TripOverviewUiState> =
            combine(
                tripRepository.observeTrip(route.tripId),
                tripRepository.observeMembers(route.tripId),
                authRepository.observeAuthState(),
                noteState,
            ) { trip, members, user, note ->
                TripOverviewUiState(
                    isLoading = trip == null,
                    trip = trip,
                    members = members,
                    isOwner = trip != null && user != null && trip.ownerId == user.id,
                    noteDraft = if (note.touched) note.draft else trip?.note.orEmpty(),
                    isSavingNote = note.saving,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = TripOverviewUiState(),
            )

        fun onNoteChange(note: String) {
            noteState.update { it.copy(draft = note, touched = true) }
        }

        fun saveNote() {
            val draft = noteState.value.draft
            viewModelScope.launch {
                noteState.update { it.copy(saving = true) }
                tripRepository.updateTripNote(route.tripId, draft.ifBlank { null })
                    .onSuccess { noteState.update { it.copy(touched = false, saving = false) } }
                    .onFailure {
                        noteState.update { it.copy(saving = false) }
                        _events.send(TripOverviewEvent.Message(R.string.overview_note_error))
                    }
            }
        }

        fun archiveTrip() {
            viewModelScope.launch {
                tripRepository.setArchived(route.tripId, archived = true)
                    .onSuccess { _events.send(TripOverviewEvent.Message(R.string.overview_archived)) }
                    .onFailure { _events.send(TripOverviewEvent.Message(R.string.overview_action_error)) }
            }
        }

        /** M6.4 — a member without an account, added by name; money maths treats them like anyone else. */
        fun addGuestMember(displayName: String) {
            val trimmed = displayName.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                tripRepository.addGuestMember(route.tripId, trimmed)
                    .onSuccess { _events.send(TripOverviewEvent.Message(R.string.overview_guest_added)) }
                    .onFailure { _events.send(TripOverviewEvent.Message(R.string.overview_action_error)) }
            }
        }

        /** Owner-only; client-side cascade delete. Emits [TripOverviewEvent.Deleted] to leave the screen. */
        fun deleteTrip() {
            viewModelScope.launch {
                tripRepository.deleteTrip(route.tripId)
                    .onSuccess { _events.send(TripOverviewEvent.Deleted) }
                    .onFailure { _events.send(TripOverviewEvent.Message(R.string.overview_action_error)) }
            }
        }

        fun closeInvite() {
            val code = uiState.value.trip?.inviteCode ?: return
            viewModelScope.launch {
                tripRepository.setInviteActive(code, active = false)
                    .onSuccess { _events.send(TripOverviewEvent.Message(R.string.overview_invite_closed)) }
                    .onFailure { _events.send(TripOverviewEvent.Message(R.string.overview_action_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
