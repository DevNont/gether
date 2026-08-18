package com.triptogether.feature.expense

import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import kotlinx.datetime.LocalDate

data class ExpenseDayGroup(
    val date: LocalDate,
    val total: Money,
    val expenses: List<Expense>,
)

data class ExpenseListUiState(
    val isLoading: Boolean = true,
    val groups: List<ExpenseDayGroup> = emptyList(),
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
    val filter: ExpenseCategory? = null,
    val tripTotal: Money = Money.ZERO,
    val myTotal: Money = Money.ZERO,
) {
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()

    fun memberName(memberId: String): String = members.firstOrNull { it.id == memberId }?.displayName ?: ""
}
