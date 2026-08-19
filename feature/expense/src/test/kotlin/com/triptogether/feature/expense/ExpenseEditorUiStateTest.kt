package com.triptogether.feature.expense

import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.SplitMode
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Pure state tests — no ViewModel; ExpenseEditorUiState derives everything itself. */
class ExpenseEditorUiStateTest {
    private fun member(id: String) = Member(id = id, userId = id, displayName = id)

    private fun state(
        splitMode: SplitMode,
        rows: List<MemberSplitRow>,
        totalInput: String = "",
    ) = ExpenseEditorUiState(
        isLoading = false,
        title = "dinner",
        totalInput = totalInput,
        paidByMemberId = "m1",
        date = LocalDate(2026, 12, 5),
        splitMode = splitMode,
        rows = rows,
    )

    @Test
    @DisplayName("ITEMIZED: total and computedShares derive from the entered rows; blank rows count as zero")
    fun itemizedTotalDerivesFromRows() {
        val s =
            state(
                splitMode = SplitMode.ITEMIZED,
                rows =
                    listOf(
                        MemberSplitRow(member("m1"), amountInput = "120.50"),
                        MemberSplitRow(member("m2"), amountInput = "79.50"),
                        MemberSplitRow(member("m3"), amountInput = ""),
                    ),
            )

        assertEquals(20_000L, s.total?.satang)
        val shares = s.computedShares
        assertEquals(20_000L, shares?.sumOf { it.amount.satang })
        assertEquals(0L, shares?.first { it.memberId == "m3" }?.amount?.satang)
        assertTrue(s.canSave)
    }

    @Test
    @DisplayName("ITEMIZED: cannot save until at least one row has an amount")
    fun itemizedNeedsAtLeastOneEnteredRow() {
        val s =
            state(
                splitMode = SplitMode.ITEMIZED,
                rows =
                    listOf(
                        MemberSplitRow(member("m1"), amountInput = ""),
                        MemberSplitRow(member("m2"), amountInput = ""),
                    ),
            )

        assertEquals(0L, s.total?.satang)
        assertFalse(s.canSave)
    }

    @Test
    @DisplayName("ITEMIZED: an unparseable amount blocks saving")
    fun itemizedInvalidRowBlocksSave() {
        val s =
            state(
                splitMode = SplitMode.ITEMIZED,
                rows =
                    listOf(
                        MemberSplitRow(member("m1"), amountInput = "100"),
                        MemberSplitRow(member("m2"), amountInput = "abc"),
                    ),
            )

        assertTrue(s.rows[1].isInvalid)
        assertFalse(s.canSave)
    }

    @Test
    @DisplayName("EXACT: canSave only when every selected row is entered and the delta is zero")
    fun exactCanSaveGatedByDelta() {
        val rowsBalanced =
            listOf(
                MemberSplitRow(member("m1"), amountInput = "100"),
                MemberSplitRow(member("m2"), amountInput = "100"),
                MemberSplitRow(member("m3"), amountInput = "100"),
            )
        val balanced = state(splitMode = SplitMode.EXACT, rows = rowsBalanced, totalInput = "300")
        assertEquals(0L, balanced.delta?.satang)
        assertTrue(balanced.canSave)

        val short =
            balanced.copy(
                rows = rowsBalanced.map { if (it.member.id == "m3") it.copy(amountInput = "90") else it },
            )
        assertFalse(short.delta?.satang == 0L)
        assertFalse(short.canSave)

        val incomplete =
            balanced.copy(
                rows = rowsBalanced.map { if (it.member.id == "m3") it.copy(amountInput = "") else it },
            )
        assertFalse(incomplete.everyoneEntered)
        assertFalse(incomplete.canSave)
    }

    @Test
    @DisplayName("EXACT: an unselected row does not participate in the delta")
    fun exactIgnoresUnselectedRows() {
        val s =
            state(
                splitMode = SplitMode.EXACT,
                rows =
                    listOf(
                        MemberSplitRow(member("m1"), amountInput = "150"),
                        MemberSplitRow(member("m2"), amountInput = "150"),
                        MemberSplitRow(member("m3"), selected = false),
                    ),
                totalInput = "300",
            )

        assertEquals(0L, s.delta?.satang)
        assertTrue(s.canSave)
    }
}
