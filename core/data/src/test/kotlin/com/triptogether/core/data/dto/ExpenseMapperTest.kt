package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ExpenseMapperTest {
    private val expense =
        Expense(
            id = "e1",
            title = "มื้อเย็น — ร้านหมูกระทะ",
            category = ExpenseCategory.FOOD,
            totalAmount = Money(185_000),
            paidByMemberId = "m1",
            date = LocalDate(2026, 12, 5),
            splitMode = SplitMode.EXACT,
            shares =
                listOf(
                    Share(memberId = "m1", amount = Money(100_000)),
                    Share(memberId = "m2", amount = Money(85_000)),
                ),
            slipUrls = listOf("https://x/slip.jpg"),
            note = "รวมน้ำแข็ง",
            createdBy = "m1",
        )

    @Test
    @DisplayName("Expense round-trips with money staying Long satang end to end")
    fun roundTrip() {
        val dto = expense.toDto()
        assertEquals(185_000L, dto.totalAmount)
        assertEquals(100_000L, dto.shares[0].amount)

        val back = checkNotNull(dto.toDomain("e1"))
        assertEquals(expense, back)
        assertTrue(back.isBalanced)
    }

    @Test
    @DisplayName("Category maps to lowercase wire names and unknown values fall back to OTHER")
    fun categoryMapping() {
        assertEquals("food", ExpenseCategory.FOOD.toWireName())
        assertEquals("stay", ExpenseCategory.STAY.toWireName())
        assertEquals(
            ExpenseCategory.TRANSPORT,
            checkNotNull(ExpenseDto(category = "transport", date = "2026-01-01").toDomain(id = "x")).category,
        )
        assertEquals(
            ExpenseCategory.OTHER,
            checkNotNull(ExpenseDto(category = "???", date = "2026-01-01").toDomain(id = "x")).category,
        )
    }

    @Test
    @DisplayName("Unknown splitMode falls back to EQUAL, weights survive SHARES mode")
    fun splitModeMapping() {
        assertEquals(
            SplitMode.EQUAL,
            checkNotNull(ExpenseDto(splitMode = "junk", date = "2026-01-01").toDomain("x")).splitMode,
        )

        val weighted =
            expense.copy(
                splitMode = SplitMode.SHARES,
                shares =
                    listOf(
                        Share(memberId = "m1", amount = Money(123_400), weight = 2),
                        Share(memberId = "m2", amount = Money(61_600), weight = 1),
                    ),
            )
        val back = checkNotNull(weighted.toDto().toDomain("e1"))
        assertEquals(2, back.shares[0].weight)
        assertEquals(SplitMode.SHARES, back.splitMode)
    }

    @Test
    @DisplayName("Unbalanced dto still maps but isBalanced flags it for the repository guard")
    fun unbalancedDetectable() {
        val unbalanced =
            expense.toDto().copy(
                shares = listOf(ShareDto(memberId = "m1", amount = 1)),
            )
        assertEquals(false, checkNotNull(unbalanced.toDomain("e1")).isBalanced)
    }

    @Test
    @DisplayName("Corrupt date string maps to null instead of throwing")
    fun corruptDateMapsToNull() {
        assertNull(ExpenseDto(date = "").toDomain("x"))
        assertNull(ExpenseDto(date = "not-a-date").toDomain("x"))
    }
}
