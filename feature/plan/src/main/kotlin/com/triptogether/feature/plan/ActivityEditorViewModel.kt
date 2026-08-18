package com.triptogether.feature.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.PlanRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.plan.navigation.ActivityEditorRoute
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
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
class ActivityEditorViewModel
    @Inject
    constructor(
        private val planRepository: PlanRepository,
        private val tripRepository: TripRepository,
        private val authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ActivityEditorRoute>()

        private val _uiState = MutableStateFlow(ActivityEditorUiState(isLoading = route.activityId != null))
        val uiState: StateFlow<ActivityEditorUiState> = _uiState.asStateFlow()

        private val _events = Channel<ActivityEditorEvent>(Channel.BUFFERED)
        val events: Flow<ActivityEditorEvent> = _events.receiveAsFlow()

        /** Preserved from the loaded activity so an edit never resets ordering or authorship. */
        private var existing: Activity? = null

        init {
            if (route.activityId != null) loadExisting(route.activityId)
        }

        private fun loadExisting(activityId: String) {
            viewModelScope.launch {
                val activity =
                    planRepository.observeDayPlan(route.tripId, route.dayId).first()
                        ?.activities?.firstOrNull { it.id == activityId }
                existing = activity
                _uiState.update {
                    if (activity == null) {
                        it.copy(isLoading = false)
                    } else {
                        it.copy(
                            title = activity.title,
                            startTime = activity.startTime,
                            endTime = activity.endTime,
                            placeName = activity.placeName.orEmpty(),
                            note = activity.note.orEmpty(),
                            isExisting = true,
                            isLoading = false,
                        )
                    }
                }
            }
        }

        fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

        fun onPlaceChange(value: String) = _uiState.update { it.copy(placeName = value) }

        fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

        fun onStartTimeChange(value: LocalTime?) = _uiState.update { it.copy(startTime = value) }

        fun onEndTimeChange(value: LocalTime?) = _uiState.update { it.copy(endTime = value) }

        fun save() {
            val state = _uiState.value
            if (!state.canSave) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val activity =
                    Activity(
                        id = existing?.id ?: "",
                        title = state.title.trim(),
                        startTime = state.startTime,
                        endTime = state.endTime,
                        placeName = state.placeName.trim().ifBlank { null },
                        note = state.note.trim().ifBlank { null },
                        attachments = existing?.attachments ?: emptyList(),
                        sortOrder = existing?.sortOrder ?: nextSortOrder(),
                        createdBy = existing?.createdBy ?: currentMemberId(),
                    )
                planRepository.upsertActivity(route.tripId, route.dayId, activity)
                    .onSuccess { _events.send(ActivityEditorEvent.Saved) }
                    .onFailure { _events.send(ActivityEditorEvent.Error(R.string.activity_editor_error)) }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        fun delete() {
            val id = existing?.id ?: return
            viewModelScope.launch {
                planRepository.deleteActivity(route.tripId, route.dayId, id)
                    .onSuccess { _events.send(ActivityEditorEvent.Deleted) }
                    .onFailure { _events.send(ActivityEditorEvent.Error(R.string.activity_editor_error)) }
            }
        }

        private suspend fun nextSortOrder(): Int =
            (
                planRepository.observeDayPlan(route.tripId, route.dayId).first()
                    ?.activities?.maxOfOrNull { it.sortOrder } ?: -1
            ) + 1

        private suspend fun currentMemberId(): String {
            val userId = authRepository.observeAuthState().filterNotNull().first().id
            return tripRepository.observeMembers(route.tripId).first()
                .firstOrNull { it.userId == userId }?.id ?: userId
        }
    }
