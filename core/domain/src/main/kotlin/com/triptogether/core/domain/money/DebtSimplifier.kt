package com.triptogether.core.domain.money

import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Transfer

/**
 * Turns a trip's expenses into "who pays whom". Implements docs/04-money-logic.md
 * sections 3 and 4.
 */
object BalanceCalculator {
    /**
     * Net position per member. Positive means the others owe them.
     *
     * Only CONFIRMED settlements move money — a transfer someone marked as sent
     * but the recipient has not acknowledged is shown in the UI as pending and
     * deliberately left out of the maths.
     *
     * The returned amounts always sum to exactly zero; [assertBalanced] checks it.
     */
    fun calculate(
        memberIds: List<String>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): List<Balance> {
        val net = memberIds.associateWith { 0L }.toMutableMap()

        for (expense in expenses) {
            net.merge(expense.paidByMemberId, expense.totalAmount.satang, Long::plus)
            for (share in expense.shares) {
                net.merge(share.memberId, -share.amount.satang, Long::plus)
            }
        }

        for (settlement in settlements) {
            if (settlement.status != SettlementStatus.CONFIRMED) continue
            net.merge(settlement.fromMemberId, settlement.amount.satang, Long::plus)
            net.merge(settlement.toMemberId, -settlement.amount.satang, Long::plus)
        }

        return net.entries
            .sortedBy { it.key }
            .map { Balance(memberId = it.key, amount = Money(it.value)) }
    }

    /** Invariant from money-logic section 3. Call it in tests and log it in production. */
    fun assertBalanced(balances: List<Balance>) {
        val sum = balances.sumOf { it.amount.satang }
        check(sum == 0L) { "Balances must sum to zero but summed to $sum satang" }
    }
}

object DebtSimplifier {
    /**
     * Reduces a set of balances to the shortest practical list of transfers,
     * ignoring who originally owed whom: if A owes B and B owes C, A pays C.
     *
     * Greedy max-max matching — repeatedly pair the largest creditor with the
     * largest debtor. This produces at most (members with a non-zero balance − 1)
     * transfers. Finding the true minimum is NP-hard (it is subset-sum in
     * disguise), but at trip scale the greedy result is optimal or within a
     * transfer or two of it, which is not worth the extra complexity.
     *
     * Ties are broken by memberId so every device renders the same list.
     */
    fun simplify(balances: List<Balance>): List<Transfer> {
        val creditors =
            balances
                .filter { it.amount.isPositive }
                .sortedWith(compareByDescending<Balance> { it.amount.satang }.thenBy { it.memberId })
                .map { it.memberId to it.amount.satang }
                .toMutableList()

        val debtors =
            balances
                .filter { it.amount.isNegative }
                .sortedWith(compareBy<Balance> { it.amount.satang }.thenBy { it.memberId })
                .map { it.memberId to -it.amount.satang }
                .toMutableList()

        val transfers = mutableListOf<Transfer>()

        var ci = 0
        var di = 0
        var creditorLeft = if (creditors.isEmpty()) 0L else creditors[0].second
        var debtorLeft = if (debtors.isEmpty()) 0L else debtors[0].second

        while (ci < creditors.size && di < debtors.size) {
            val amount = minOf(creditorLeft, debtorLeft)
            if (amount > 0) {
                transfers +=
                    Transfer(
                        fromMemberId = debtors[di].first,
                        toMemberId = creditors[ci].first,
                        amount = Money(amount),
                    )
            }
            creditorLeft -= amount
            debtorLeft -= amount
            if (creditorLeft == 0L) {
                ci++
                if (ci < creditors.size) creditorLeft = creditors[ci].second
            }
            if (debtorLeft == 0L) {
                di++
                if (di < debtors.size) debtorLeft = debtors[di].second
            }
        }

        return transfers
    }
}
