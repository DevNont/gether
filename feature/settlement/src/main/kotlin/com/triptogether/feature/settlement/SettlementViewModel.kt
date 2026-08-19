package com.triptogether.feature.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Transfer
import com.triptogether.core.domain.repository.AnalyticsLogger
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import com.triptogether.core.domain.usecase.GetBalancesUseCase
import com.triptogether.core.domain.usecase.GetSuggestedTransfersUseCase
import com.triptogether.feature.settlement.navigation.SettlementRoute
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

@HiltViewModel
class SettlementViewModel
    @Inject
    constructor(
        getBalances: GetBalancesUseCase,
        getTransfers: GetSuggestedTransfersUseCase,
        private val settlementRepository: SettlementRepository,
        tripRepository: TripRepository,
        expenseRepository: ExpenseRepository,
        authRepository: AuthRepository,
        private val analytics: AnalyticsLogger,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<SettlementRoute>()

        private val _events = Channel<SettlementEvent>(Channel.BUFFERED)
        val events: Flow<SettlementEvent> = _events.receiveAsFlow()

        private data class MoneySnapshot(
            val balances: List<Balance>,
            val transfers: List<Transfer>,
            val settlements: List<Settlement>,
        )

        private val moneyFlow =
            combine(
                getBalances(route.tripId),
                getTransfers(route.tripId),
                settlementRepository.observeSettlements(route.tripId),
            ) { balances, transfers, settlements -> MoneySnapshot(balances, transfers, settlements) }

        val uiState: StateFlow<SettlementUiState> =
            combine(
                moneyFlow,
                tripRepository.observeMembers(route.tripId),
                expenseRepository.observeExpenses(route.tripId),
                authRepository.observeAuthState(),
            ) { money, members, expenses, user ->
                val tripTotal = Money(expenses.sumOf { it.totalAmount.satang })
                SettlementUiState(
                    isLoading = false,
                    balances = money.balances,
                    transfers = money.transfers,
                    settlements = money.settlements,
                    members = members,
                    myMemberId = members.firstOrNull { it.userId == user?.id }?.id,
                    tripTotal = tripTotal,
                    perPersonAverage =
                        if (members.isEmpty()) Money.ZERO else Money(tripTotal.satang / members.size),
                )
            }.catch {
                // A failed listener (e.g. PERMISSION_DENIED) must surface, not render as an empty screen.
                _events.send(SettlementEvent.Message(R.string.settlement_error))
                emit(SettlementUiState(isLoading = false))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = SettlementUiState(),
            )

        /** "โอนแล้ว" — records the transfer as PENDING until the receiver confirms. */
        fun markTransferred(transfer: Transfer) {
            val me = uiState.value.myMemberId ?: return
            viewModelScope.launch {
                settlementRepository.create(
                    route.tripId,
                    Settlement(
                        id = "",
                        fromMemberId = transfer.fromMemberId,
                        toMemberId = transfer.toMemberId,
                        amount = transfer.amount,
                        status = SettlementStatus.PENDING,
                        markedBy = me,
                    ),
                )
                    .onSuccess { _events.send(SettlementEvent.Message(R.string.settlement_marked)) }
                    .onFailure { _events.send(SettlementEvent.Message(R.string.settlement_mark_error)) }
            }
        }

        /** Receiver-side confirmation flips PENDING to CONFIRMED and moves the money in the maths. */
        fun confirm(settlementId: String) {
            val me = uiState.value.myMemberId ?: return
            viewModelScope.launch {
                settlementRepository.confirm(route.tripId, settlementId, me)
                    .onSuccess {
                        analytics.log(AnalyticsLogger.SETTLEMENT_CONFIRMED)
                        _events.send(SettlementEvent.Message(R.string.settlement_confirmed))
                    }
                    .onFailure { _events.send(SettlementEvent.Message(R.string.settlement_confirm_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
