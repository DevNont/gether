package com.triptogether.feature.expense

import androidx.annotation.StringRes
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.domain.money.ExpenseSplitter
import kotlinx.datetime.LocalDate

data class MemberSplitRow(
    val member: Member,
    val selected: Boolean = true,
    /** EXACT mode free input; blank means "not entered yet", which is not the same as 0. */
    val amountInput: String = "",
    /** SHARES mode weight, always >= 1. */
    val weight: Int = 1,
) {
    val enteredAmount: Money? get() = Money.parse(amountInput.trim()).takeIf { amountInput.isNotBlank() }
}

data class ExpenseEditorUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val totalInput: String = "",
    val paidByMemberId: String? = null,
    val date: LocalDate? = null,
    val splitMode: SplitMode = SplitMode.EQUAL,
    val rows: List<MemberSplitRow> = emptyList(),
    val isExisting: Boolean = false,
    val isSaving: Boolean = false,
    /** First day of the trip; a bill dated before it is a prepaid expense. */
    val tripStart: LocalDate? = null,
) {
    val isPrepaid: Boolean get() = date != null && tripStart != null && date < tripStart

    val total: Money? get() = Money.parse(totalInput.trim())

    val selectedRows: List<MemberSplitRow> get() = rows.filter { it.selected }

    /** Shares as the current mode computes them; null when input is not complete enough. */
    val computedShares: List<Share>?
        get() {
            val total = total ?: return null
            val selected = selectedRows
            if (selected.isEmpty()) return null
            return when (splitMode) {
                SplitMode.EQUAL -> ExpenseSplitter.splitEqually(total, selected.map { it.member.id })
                SplitMode.SHARES ->
                    ExpenseSplitter.splitByWeights(total, selected.associate { it.member.id to it.weight })
                SplitMode.EXACT ->
                    selected.map { Share(memberId = it.member.id, amount = it.enteredAmount ?: Money.ZERO) }
            }
        }

    /** EXACT-mode difference between entered shares and the total; zero for computed modes. */
    val delta: Money?
        get() {
            if (splitMode != SplitMode.EXACT) return Money.ZERO
            val total = total ?: return null
            val entered = selectedRows.mapNotNull { it.enteredAmount }
            return ExpenseSplitter.validationDelta(total, entered.map { Share("", it) })
        }

    val everyoneEntered: Boolean
        get() = selectedRows.isNotEmpty() && selectedRows.all { it.enteredAmount != null }

    val canSave: Boolean
        get() {
            val total = total ?: return false
            if (title.isBlank() || total.isNegative || isSaving) return false
            if (paidByMemberId == null || date == null) return false
            if (selectedRows.isEmpty()) return false
            return when (splitMode) {
                SplitMode.EQUAL, SplitMode.SHARES -> true
                SplitMode.EXACT -> everyoneEntered && delta?.isZero == true
            }
        }

    fun computedAmountFor(memberId: String): Money? = computedShares?.firstOrNull { it.memberId == memberId }?.amount
}

sealed interface ExpenseEditorEvent {
    data object Saved : ExpenseEditorEvent

    data class Error(
        @StringRes val messageResId: Int,
    ) : ExpenseEditorEvent
}
