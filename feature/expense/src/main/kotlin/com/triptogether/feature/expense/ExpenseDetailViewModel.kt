package com.triptogether.feature.expense

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.MemberRole
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.expense.navigation.ExpenseDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val members: List<Member> = emptyList(),
    val canEdit: Boolean = false,
) {
    fun memberName(memberId: String): String = members.firstOrNull { it.id == memberId }?.displayName ?: ""
}

sealed interface ExpenseDetailEvent {
    data object Deleted : ExpenseDetailEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : ExpenseDetailEvent
}

@HiltViewModel
class ExpenseDetailViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        tripRepository: TripRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ExpenseDetailRoute>()

        private val _events = Channel<ExpenseDetailEvent>(Channel.BUFFERED)
        val events: Flow<ExpenseDetailEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<ExpenseDetailUiState> =
            combine(
                expenseRepository.observeExpenses(route.tripId),
                tripRepository.observeMembers(route.tripId),
                authRepository.observeAuthState(),
            ) { expenses, members, user ->
                val expense = expenses.firstOrNull { it.id == route.expenseId }
                val myMember = members.firstOrNull { it.userId == user?.id }
                // Edit/delete for the bill's creator or the trip owner only (S10 spec).
                val canEdit =
                    myMember != null &&
                        (expense?.createdBy == myMember.id || myMember.role == MemberRole.OWNER)
                ExpenseDetailUiState(
                    isLoading = false,
                    expense = expense,
                    members = members,
                    canEdit = canEdit,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ExpenseDetailUiState(),
            )

        fun delete() {
            viewModelScope.launch {
                expenseRepository.delete(route.tripId, route.expenseId)
                    .onSuccess { _events.send(ExpenseDetailEvent.Deleted) }
                    .onFailure { _events.send(ExpenseDetailEvent.Error(R.string.expense_detail_delete_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
