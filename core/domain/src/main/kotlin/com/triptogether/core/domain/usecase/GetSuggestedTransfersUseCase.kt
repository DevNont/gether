package com.triptogether.core.domain.usecase

import com.triptogether.core.domain.model.Transfer
import com.triptogether.core.domain.money.DebtSimplifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Live "who pays whom" list derived from the balances (docs/04 §4). */
class GetSuggestedTransfersUseCase(
    private val getBalances: GetBalancesUseCase,
) {
    operator fun invoke(tripId: String): Flow<List<Transfer>> = getBalances(tripId).map { DebtSimplifier.simplify(it) }
}
