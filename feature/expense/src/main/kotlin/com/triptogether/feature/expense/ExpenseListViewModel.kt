package com.triptogether.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.expense.navigation.ExpenseListRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel
    @Inject
    constructor(
        expenseRepository: ExpenseRepository,
        tripRepository: TripRepository,
        authRepository: AuthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ExpenseListRoute>()
        private val filter = MutableStateFlow<ExpenseCategory?>(null)

        val uiState: StateFlow<ExpenseListUiState> =
            combine(
                expenseRepository.observeExpenses(route.tripId),
                tripRepository.observeMembers(route.tripId),
                authRepository.observeAuthState(),
                filter,
            ) { expenses, members, user, filter ->
                buildState(expenses, members, user, filter)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ExpenseListUiState(),
            )

        fun setFilter(category: ExpenseCategory?) {
            filter.value = category
        }

        private fun buildState(
            expenses: List<Expense>,
            members: List<Member>,
            user: User?,
            filter: ExpenseCategory?,
        ): ExpenseListUiState {
            val myMemberId = members.firstOrNull { it.userId == user?.id }?.id
            val filtered = if (filter == null) expenses else expenses.filter { it.category == filter }
            val groups =
                filtered
                    .groupBy { it.date }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, dayExpenses) ->
                        ExpenseDayGroup(
                            date = date,
                            total = Money(dayExpenses.sumOf { it.totalAmount.satang }),
                            expenses = dayExpenses,
                        )
                    }
            // Header totals always cover the whole trip, not the active filter.
            return ExpenseListUiState(
                isLoading = false,
                groups = groups,
                members = members,
                myMemberId = myMemberId,
                filter = filter,
                tripTotal = Money(expenses.sumOf { it.totalAmount.satang }),
                myTotal =
                    Money(
                        myMemberId?.let { id -> expenses.sumOf { it.shareOf(id).satang } } ?: 0L,
                    ),
            )
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
