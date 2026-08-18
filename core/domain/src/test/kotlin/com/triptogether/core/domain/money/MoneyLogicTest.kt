package com.triptogether.core.domain.money

import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Covers T1-T20 from docs/04-money-logic.md section 5.
 * These are the tests that must stay green — a change that breaks one of them
 * is a change that quietly loses someone's money.
 */
class MoneyLogicTest {
    private val date = LocalDate(2026, 12, 12)

    @Nested
    @DisplayName("Equal split")
    inner class EqualSplit {
        @Test fun `T1 - divides evenly`() {
            val shares = ExpenseSplitter.splitEqually(Money(10_000), listOf("a", "b", "c", "d"))
            assertEquals(listOf(2500L, 2500L, 2500L, 2500L), shares.map { it.amount.satang })
        }

        @Test fun `T2 - remainder goes to first ids by sort order`() {
            val shares = ExpenseSplitter.splitEqually(Money(10_000), listOf("c", "a", "b"))
            assertEquals(10_000L, shares.sumOf { it.amount.satang })
            assertEquals(3334L, shares.first { it.memberId == "a" }.amount.satang)
            assertEquals(3333L, shares.first { it.memberId == "b" }.amount.satang)
            assertEquals(3333L, shares.first { it.memberId == "c" }.amount.satang)
        }

        @Test fun `T3 - spread never exceeds one satang`() {
            val ids = (1..7).map { "m$it" }
            val shares = ExpenseSplitter.splitEqually(Money(10_000), ids)
            val amounts = shares.map { it.amount.satang }
            assertEquals(10_000L, amounts.sum())
            assertEquals(1L, amounts.max() - amounts.min())
        }

        @Test fun `T4 - single member takes it all`() {
            val shares = ExpenseSplitter.splitEqually(Money(10_000), listOf("a"))
            assertEquals(listOf(10_000L), shares.map { it.amount.satang })
        }

        @Test fun `T5 - zero total gives zero shares`() {
            val shares = ExpenseSplitter.splitEqually(Money.ZERO, listOf("a", "b", "c"))
            assertTrue(shares.all { it.amount.isZero })
        }

        @Test fun `T6 - one satang between three people`() {
            val shares = ExpenseSplitter.splitEqually(Money(1), listOf("a", "b", "c"))
            assertEquals(1L, shares.sumOf { it.amount.satang })
            assertEquals(1L, shares.first { it.memberId == "a" }.amount.satang)
        }
    }

    @Nested
    @DisplayName("Exact split validation")
    inner class ExactSplit {
        private val total = Money(185_000)

        @Test fun `T7 - matching shares validate`() {
            val shares =
                listOf(52_000L, 38_000L, 45_000L, 50_000L)
                    .mapIndexed { i, v -> Share("m$i", Money(v)) }
            assertEquals(0L, ExpenseSplitter.validationDelta(total, shares).satang)
        }

        @Test fun `T8 - shortfall reported as negative`() {
            val shares =
                listOf(52_000L, 38_000L, 45_000L, 30_000L)
                    .mapIndexed { i, v -> Share("m$i", Money(v)) }
            assertEquals(-20_000L, ExpenseSplitter.validationDelta(total, shares).satang)
        }

        @Test fun `T9 - overage reported as positive`() {
            val shares =
                listOf(52_000L, 38_000L, 45_000L, 70_000L)
                    .mapIndexed { i, v -> Share("m$i", Money(v)) }
            assertEquals(20_000L, ExpenseSplitter.validationDelta(total, shares).satang)
        }
    }

    @Nested
    @DisplayName("Weighted split")
    inner class WeightedSplit {
        @Test fun `T10 - proportional to weights`() {
            val shares =
                ExpenseSplitter.splitByWeights(
                    Money(10_000),
                    mapOf("a" to 1, "b" to 1, "c" to 2),
                )
            assertEquals(2500L, shares.first { it.memberId == "a" }.amount.satang)
            assertEquals(2500L, shares.first { it.memberId == "b" }.amount.satang)
            assertEquals(5000L, shares.first { it.memberId == "c" }.amount.satang)
        }

        @Test fun `T11 - remainder still sums to total`() {
            val shares =
                ExpenseSplitter.splitByWeights(
                    Money(10_000),
                    mapOf("a" to 1, "b" to 1, "c" to 1),
                )
            assertEquals(10_000L, shares.sumOf { it.amount.satang })
        }
    }

    @Nested
    @DisplayName("Balances and settlement")
    inner class Balances {
        @Test fun `T12 - balances always sum to zero`() {
            val members = (1..5).map { "m$it" }
            val rng = Random(20261212)
            val expenses =
                (1..20).map { i ->
                    val total = Money(rng.nextLong(5_000, 500_000))
                    val participants = members.shuffled(rng).take(rng.nextInt(2, 6))
                    Expense(
                        id = "e$i",
                        title = "bill $i",
                        category = ExpenseCategory.FOOD,
                        totalAmount = total,
                        paidByMemberId = members.random(rng),
                        date = date,
                        splitMode = SplitMode.EQUAL,
                        shares = ExpenseSplitter.splitEqually(total, participants),
                        createdBy = "m1",
                    )
                }
            expenses.forEach { assertTrue(it.isBalanced, "expense ${it.id} is not balanced") }
            val balances = BalanceCalculator.calculate(members, expenses, emptyList())
            BalanceCalculator.assertBalanced(balances)
        }

        @Test fun `T13 and T14 - simplify clears everyone in at most n-1 transfers`() {
            val balances =
                listOf(
                    Balance("boss", Money(-72_000)),
                    Balance("fah", Money(-83_000)),
                    Balance("keng", Money(-30_000)),
                    Balance("mind", Money(61_000)),
                    Balance("ton", Money(124_000)),
                )
            val transfers = DebtSimplifier.simplify(balances)

            assertTrue(transfers.size <= 4, "expected at most 4 transfers, got ${transfers.size}")

            val after = balances.associate { it.memberId to it.amount.satang }.toMutableMap()
            for (t in transfers) {
                after.merge(t.fromMemberId, t.amount.satang, Long::plus)
                after.merge(t.toMemberId, -t.amount.satang, Long::plus)
            }
            assertTrue(after.values.all { it == 0L }, "not everyone was cleared: $after")
        }

        @Test fun `T15 - simplify is deterministic`() {
            val balances =
                listOf(
                    Balance("e", Money(-40_000)),
                    Balance("a", Money(15_000)),
                    Balance("d", Money(-15_000)),
                    Balance("b", Money(15_000)),
                    Balance("c", Money(25_000)),
                )
            val first = DebtSimplifier.simplify(balances)
            repeat(100) {
                assertEquals(first, DebtSimplifier.simplify(balances.shuffled()))
            }
        }

        @Test fun `T16 - nothing owed means nothing to transfer`() {
            val balances = listOf(Balance("a", Money.ZERO), Balance("b", Money.ZERO))
            assertTrue(DebtSimplifier.simplify(balances).isEmpty())
        }

        @Test fun `pending settlements do not move money`() {
            val expenses =
                listOf(
                    Expense(
                        id = "e1", title = "dinner", category = ExpenseCategory.FOOD,
                        totalAmount = Money(10_000), paidByMemberId = "a", date = date,
                        splitMode = SplitMode.EQUAL,
                        shares = ExpenseSplitter.splitEqually(Money(10_000), listOf("a", "b")),
                        createdBy = "a",
                    ),
                )
            val pending = Settlement("s1", "b", "a", Money(5_000), SettlementStatus.PENDING, "b")
            val withPending = BalanceCalculator.calculate(listOf("a", "b"), expenses, listOf(pending))
            assertEquals(5_000L, withPending.first { it.memberId == "a" }.amount.satang)

            val confirmed = pending.copy(status = SettlementStatus.CONFIRMED, confirmedBy = "a")
            val withConfirmed = BalanceCalculator.calculate(listOf("a", "b"), expenses, listOf(confirmed))
            assertTrue(withConfirmed.all { it.amount.isZero })
        }
    }

    @Nested
    @DisplayName("Editor helpers")
    inner class Helpers {
        @Test fun `T17 - distributing the remainder fills the bill exactly`() {
            val total = Money(185_000)
            val entered = listOf(Share("a", Money(52_000)), Share("b", Money(38_000)))
            val result = ExpenseSplitter.distributeRemainder(total, entered, listOf("c", "d", "e"))
            assertEquals(185_000L, result.sumOf { it.amount.satang })
            assertEquals(5, result.size)
        }

        @Test fun `T18 - surcharge keeps proportions and returns the new total`() {
            val shares =
                listOf(
                    Share("a", Money(52_000)),
                    Share("b", Money(38_000)),
                    Share("c", Money(45_000)),
                )
            val (updated, newTotal) = ExpenseSplitter.applySurcharge(shares, 10.0)
            assertEquals(newTotal.satang, updated.sumOf { it.amount.satang })
            assertEquals(57_200L, updated.first { it.memberId == "a" }.amount.satang)
            assertEquals(41_800L, updated.first { it.memberId == "b" }.amount.satang)
            assertEquals(49_500L, updated.first { it.memberId == "c" }.amount.satang)
        }
    }

    @Test
    @DisplayName("T19 - property test: shares always sum to the total")
    fun propertySharesAlwaysSumToTotal() {
        val rng = Random(4242)
        repeat(10_000) {
            val total = Money(rng.nextLong(0, 10_000_000))
            val n = rng.nextInt(1, 16)
            val ids = (1..n).map { "member-${rng.nextInt(100_000)}-$it" }
            val shares = ExpenseSplitter.splitEqually(total, ids)
            assertEquals(total.satang, shares.sumOf { it.amount.satang })
        }
    }

    @Test
    @DisplayName("T20 - guest members participate like anyone else")
    fun guestMembersCarryBalances() {
        val expenses =
            listOf(
                Expense(
                    id = "e1", title = "lunch", category = ExpenseCategory.FOOD,
                    totalAmount = Money(30_000), paidByMemberId = "user-a", date = date,
                    splitMode = SplitMode.EQUAL,
                    shares = ExpenseSplitter.splitEqually(Money(30_000), listOf("user-a", "guest-x")),
                    createdBy = "user-a",
                ),
            )
        val balances = BalanceCalculator.calculate(listOf("user-a", "guest-x"), expenses, emptyList())
        BalanceCalculator.assertBalanced(balances)
        assertEquals(-15_000L, balances.first { it.memberId == "guest-x" }.amount.satang)
    }
}
