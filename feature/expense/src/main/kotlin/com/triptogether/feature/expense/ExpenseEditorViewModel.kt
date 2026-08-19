package com.triptogether.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.domain.money.ExpenseSplitter
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.feature.expense.navigation.ExpenseEditorRoute
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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

@HiltViewModel
class ExpenseEditorViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val tripRepository: TripRepository,
        private val authRepository: AuthRepository,
        private val analytics: AnalyticsLogger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ExpenseEditorRoute>()

        private val _uiState = MutableStateFlow(ExpenseEditorUiState())
        val uiState: StateFlow<ExpenseEditorUiState> = _uiState.asStateFlow()

        private val _events = Channel<ExpenseEditorEvent>(Channel.BUFFERED)
        val events: Flow<ExpenseEditorEvent> = _events.receiveAsFlow()

        private var existing: Expense? = null
        private var myMemberId: String? = null

        init {
            viewModelScope.launch { load() }
        }

        private suspend fun load() {
            val members = tripRepository.observeMembers(route.tripId).first()
            val trip = tripRepository.observeTrip(route.tripId).filterNotNull().first()
            val userId = authRepository.observeAuthState().filterNotNull().first().id
            myMemberId = members.firstOrNull { it.userId == userId }?.id

            val expense =
                route.expenseId?.let { id ->
                    expenseRepository.observeExpenses(route.tripId).first().firstOrNull { it.id == id }
                }
            existing = expense

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val defaultDate = if (today in trip.startDate..trip.endDate) today else trip.startDate

            _uiState.update {
                if (expense == null) {
                    it.copy(
                        isLoading = false,
                        paidByMemberId = myMemberId,
                        date = defaultDate,
                        time = null,
                        tripStart = trip.startDate,
                        rows = members.map { member -> MemberSplitRow(member = member) },
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        title = expense.title,
                        category = expense.category,
                        totalInput = expense.totalAmount.format(),
                        paidByMemberId = expense.paidByMemberId,
                        date = expense.date,
                        time = expense.time,
                        tripStart = trip.startDate,
                        splitMode = expense.splitMode,
                        isExisting = true,
                        rows =
                            members.map { member ->
                                val share = expense.shares.firstOrNull { s -> s.memberId == member.id }
                                MemberSplitRow(
                                    member = member,
                                    selected = share != null,
                                    amountInput = share?.amount?.format() ?: "",
                                    weight = share?.weight ?: 1,
                                )
                            },
                    )
                }
            }
        }

        fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

        fun onCategoryChange(value: ExpenseCategory) = _uiState.update { it.copy(category = value) }

        fun onTotalChange(value: String) = _uiState.update { it.copy(totalInput = value) }

        fun onPaidByChange(memberId: String) = _uiState.update { it.copy(paidByMemberId = memberId) }

        fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }

        fun onTimeChange(value: LocalTime?) = _uiState.update { it.copy(time = value) }

        fun onSplitModeChange(mode: SplitMode) = _uiState.update { it.copy(splitMode = mode) }

        fun onMemberToggle(memberId: String) =
            _uiState.update { state ->
                state.copy(
                    rows =
                        state.rows.map { row ->
                            if (row.member.id == memberId) row.copy(selected = !row.selected) else row
                        },
                )
            }

        fun onAmountChange(
            memberId: String,
            value: String,
        ) = _uiState.update { state ->
            state.copy(
                rows =
                    state.rows.map { row ->
                        if (row.member.id == memberId) row.copy(amountInput = value) else row
                    },
            )
        }

        fun onWeightChange(
            memberId: String,
            delta: Int,
        ) = _uiState.update { state ->
            state.copy(
                rows =
                    state.rows.map { row ->
                        if (row.member.id == memberId) {
                            row.copy(weight = (row.weight + delta).coerceAtLeast(1))
                        } else {
                            row
                        }
                    },
            )
        }

        /** เกลี่ยส่วนที่เหลือ — split what is left of the total equally between unfilled members (docs/04 §2.2). */
        fun distributeRemainder() {
            val state = _uiState.value
            val total = state.total ?: return
            val entered = state.selectedRows.filter { it.enteredAmount != null }
            val unfilledIds = state.selectedRows.filter { it.enteredAmount == null }.map { it.member.id }
            if (unfilledIds.isEmpty()) return
            val result =
                runCatching {
                    ExpenseSplitter.distributeRemainder(
                        total = total,
                        enteredShares = entered.map { Share(it.member.id, it.enteredAmount!!) },
                        remainingMemberIds = unfilledIds,
                    )
                }.getOrNull() ?: return
            _uiState.update { s ->
                s.copy(
                    rows =
                        s.rows.map { row ->
                            val share = result.firstOrNull { it.memberId == row.member.id }
                            if (row.selected && share != null) row.copy(amountInput = share.amount.format()) else row
                        },
                )
            }
        }

        /** + ค่าบริการ 10% — scale every entered share and update the total to the new sum (docs/04 §2.2). */
        fun applyServiceCharge() {
            val state = _uiState.value
            val entered =
                state.selectedRows.mapNotNull { row ->
                    row.enteredAmount?.let { Share(row.member.id, it) }
                }
            if (entered.isEmpty()) return
            val (updated, newTotal) = ExpenseSplitter.applySurcharge(entered, SERVICE_CHARGE_PERCENT)
            _uiState.update { s ->
                s.copy(
                    totalInput = newTotal.format(),
                    rows =
                        s.rows.map { row ->
                            val share = updated.firstOrNull { it.memberId == row.member.id }
                            if (share != null) row.copy(amountInput = share.amount.format()) else row
                        },
                )
            }
        }

        fun save() {
            val state = _uiState.value
            if (!state.canSave) return
            val total = state.total ?: return
            val shares = state.computedShares ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val expense =
                    Expense(
                        id = existing?.id ?: "",
                        title = state.title.trim(),
                        category = state.category,
                        totalAmount = total,
                        paidByMemberId = checkNotNull(state.paidByMemberId),
                        date = checkNotNull(state.date),
                        time = state.time,
                        splitMode = state.splitMode,
                        shares = shares,
                        slipUrls = existing?.slipUrls ?: emptyList(),
                        note = existing?.note,
                        createdBy = existing?.createdBy ?: myMemberId.orEmpty(),
                    )
                expenseRepository.upsert(route.tripId, expense)
                    .onSuccess {
                        analytics.log(
                            AnalyticsLogger.EXPENSE_SAVED,
                            mapOf("split_mode" to expense.splitMode.name),
                        )
                        _events.send(ExpenseEditorEvent.Saved)
                    }
                    .onFailure { _events.send(ExpenseEditorEvent.Error(R.string.expense_editor_error)) }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private companion object {
            const val SERVICE_CHARGE_PERCENT = 10.0
        }
    }
