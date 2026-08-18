package com.triptogether.core.domain.usecase

import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.money.BalanceCalculator
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Live net balance per member (docs/04 §3). Recomputed on every expense,
 * member, or settlement change; only CONFIRMED settlements move money.
 */
class GetBalancesUseCase(
    private val tripRepository: TripRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
) {
    operator fun invoke(tripId: String): Flow<List<Balance>> =
        combine(
            tripRepository.observeMembers(tripId),
            expenseRepository.observeExpenses(tripId),
            settlementRepository.observeSettlements(tripId),
        ) { members, expenses, settlements ->
            BalanceCalculator.calculate(
                memberIds = members.map { it.id },
                expenses = expenses,
                settlements = settlements,
            )
        }
}
