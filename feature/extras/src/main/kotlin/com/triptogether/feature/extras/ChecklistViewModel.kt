package com.triptogether.feature.extras

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.ChecklistItem
import com.triptogether.core.domain.model.ChecklistScope
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ChecklistRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.extras.navigation.ChecklistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChecklistUiState(
    val isLoading: Boolean = true,
    val shared: List<ChecklistItem> = emptyList(),
    val personal: List<ChecklistItem> = emptyList(),
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
) {
    fun isChecked(item: ChecklistItem): Boolean =
        when (item.scope) {
            // Shared: any tick shows checked for everyone.
            ChecklistScope.SHARED -> item.checkedBy.isNotEmpty()
            // Personal: shared list of items, but each member only sees their own tick.
            ChecklistScope.PERSONAL -> myMemberId != null && myMemberId in item.checkedBy
        }

    fun assignee(item: ChecklistItem): Member? = members.firstOrNull { it.id == item.assignedMemberId }
}

sealed interface ChecklistEvent {
    data class Error(
        @StringRes val messageResId: Int,
    ) : ChecklistEvent
}

@HiltViewModel
class ChecklistViewModel
    @Inject
    constructor(
        private val checklistRepository: ChecklistRepository,
        tripRepository: TripRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ChecklistRoute>()

        private val _events = Channel<ChecklistEvent>(Channel.BUFFERED)
        val events: Flow<ChecklistEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<ChecklistUiState> =
            combine(
                checklistRepository.observeItems(route.tripId),
                tripRepository.observeMembers(route.tripId),
                authRepository.observeAuthState(),
            ) { items, members, user ->
                ChecklistUiState(
                    isLoading = false,
                    shared = items.filter { it.scope == ChecklistScope.SHARED },
                    personal = items.filter { it.scope == ChecklistScope.PERSONAL },
                    members = members,
                    myMemberId = members.firstOrNull { it.userId == user?.id }?.id,
                )
            }.catch {
                // A failed listener (e.g. PERMISSION_DENIED) must surface, not render as an empty list.
                _events.send(ChecklistEvent.Error(R.string.checklist_error))
                emit(ChecklistUiState(isLoading = false))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ChecklistUiState(),
            )

        fun addItem(
            title: String,
            scope: ChecklistScope,
        ) {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return
            val existing = if (scope == ChecklistScope.SHARED) uiState.value.shared else uiState.value.personal
            viewModelScope.launch {
                checklistRepository.upsert(
                    route.tripId,
                    ChecklistItem(
                        id = "",
                        title = trimmed,
                        scope = scope,
                        sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                    ),
                ).onFailure { _events.send(ChecklistEvent.Error(R.string.checklist_error)) }
            }
        }

        fun toggle(item: ChecklistItem) {
            val me = uiState.value.myMemberId ?: return
            val checked = uiState.value.isChecked(item)
            viewModelScope.launch {
                checklistRepository.setChecked(route.tripId, item.id, me, !checked)
                    .onFailure { _events.send(ChecklistEvent.Error(R.string.checklist_error)) }
            }
        }

        fun delete(item: ChecklistItem) {
            viewModelScope.launch {
                checklistRepository.delete(route.tripId, item.id)
                    .onFailure { _events.send(ChecklistEvent.Error(R.string.checklist_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
