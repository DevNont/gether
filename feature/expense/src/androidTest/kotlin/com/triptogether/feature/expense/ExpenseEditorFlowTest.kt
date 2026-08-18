package com.triptogether.feature.expense

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.SplitMode
import org.junit.Rule
import org.junit.Test

/**
 * M4.8 — the unequal (EXACT) split flow: save stays disabled while the shares
 * do not sum to the total, the delta bar reports the gap, and the
 * distribute-remainder button completes the bill.
 *
 * Drives the real stateless content with the same state type the ViewModel
 * uses, so all money maths still comes from domain ExpenseSplitter.
 */
class ExpenseEditorFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val members =
        listOf(
            Member(id = "m1", userId = "u1", displayName = "สมชาย"),
            Member(id = "m2", userId = "u2", displayName = "สมหญิง"),
            Member(id = "m3", userId = null, displayName = "น้องแมว"),
        )

    private fun launchEditor(initial: ExpenseEditorUiState) {
        composeRule.setContent {
            var state by mutableStateOf(initial)
            ExpenseEditorContent(
                uiState = state,
                snackbarHostState = SnackbarHostState(),
                callbacks =
                    ExpenseEditorCallbacks(
                        onTitleChange = { state = state.copy(title = it) },
                        onCategoryChange = { state = state.copy(category = it) },
                        onTotalChange = { state = state.copy(totalInput = it) },
                        onPaidByChange = { state = state.copy(paidByMemberId = it) },
                        onDateChange = { state = state.copy(date = it) },
                        onSplitModeChange = { state = state.copy(splitMode = it) },
                        onMemberToggle = { id ->
                            state =
                                state.copy(
                                    rows =
                                        state.rows.map {
                                            if (it.member.id == id) it.copy(selected = !it.selected) else it
                                        },
                                )
                        },
                        onAmountChange = { id, value ->
                            state =
                                state.copy(
                                    rows =
                                        state.rows.map {
                                            if (it.member.id == id) it.copy(amountInput = value) else it
                                        },
                                )
                        },
                        onWeightChange = { _, _ -> },
                        onDistributeRemainder = {
                            val total = state.total ?: return@ExpenseEditorCallbacks
                            val entered = state.selectedRows.filter { it.enteredAmount != null }
                            val unfilled = state.selectedRows.filter { it.enteredAmount == null }
                            val result =
                                com.triptogether.core.domain.money.ExpenseSplitter.distributeRemainder(
                                    total = total,
                                    enteredShares =
                                        entered.map {
                                            com.triptogether.core.domain.model.Share(it.member.id, it.enteredAmount!!)
                                        },
                                    remainingMemberIds = unfilled.map { it.member.id },
                                )
                            state =
                                state.copy(
                                    rows =
                                        state.rows.map { row ->
                                            val share = result.firstOrNull { it.memberId == row.member.id }
                                            if (share != null) row.copy(amountInput = share.amount.format()) else row
                                        },
                                )
                        },
                        onApplyServiceCharge = {},
                        onSave = {},
                        onBack = {},
                    ),
            )
        }
    }

    private fun exactState() =
        ExpenseEditorUiState(
            isLoading = false,
            title = "มื้อเย็น",
            totalInput = "1,850.00",
            paidByMemberId = "m1",
            date = kotlinx.datetime.LocalDate(2026, 12, 5),
            splitMode = SplitMode.EXACT,
            rows = members.map { MemberSplitRow(member = it) },
        )

    @Test
    fun saveDisabledWhileSharesDoNotSumToTotal() {
        launchEditor(exactState())

        composeRule.onNodeWithTag("expense_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("amount_m1").performTextInput("520")
        composeRule.onNodeWithTag("amount_m2").performTextInput("380")
        composeRule.onNodeWithTag("amount_m3").performTextInput("300")

        // 520 + 380 + 300 = 1200 != 1850 — the save button must stay disabled.
        composeRule.onNodeWithTag("expense_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("amount_m3").performTextReplacement("950")
        composeRule.onNodeWithTag("expense_save").assertIsEnabled()
    }

    @Test
    fun distributeRemainderCompletesTheBill() {
        launchEditor(exactState())

        composeRule.onNodeWithTag("amount_m1").performTextInput("520")
        composeRule.onNodeWithTag("amount_m2").performTextInput("380")
        composeRule.onNodeWithTag("expense_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("distribute_button").performClick()

        // Remainder 950 goes to m3; the bill balances and save unlocks.
        composeRule.onNodeWithTag("expense_save").assertIsEnabled()
    }
}
