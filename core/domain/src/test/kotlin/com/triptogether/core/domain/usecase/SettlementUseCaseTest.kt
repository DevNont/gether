package com.triptogether.core.domain.usecase

import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SettlementUseCaseTest {
    private val members = MutableStateFlow(listOf(member("m1"), member("m2"), member("m3")))
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val settlements = MutableStateFlow<List<Settlement>>(emptyList())

    private val getBalances =
        GetBalancesUseCase(FakeTripRepo(members), FakeExpenseRepo(expenses), FakeSettlementRepo(settlements))
    private val getTransfers = GetSuggestedTransfersUseCase(getBalances)

    @Test
    @DisplayName("Balances react to expenses, sum to zero, and ignore PENDING settlements")
    fun balancesFollowSpec() =
        runTest {
            expenses.value =
                listOf(
                    expense(
                        total = 30_000,
                        paidBy = "m1",
                        shares = listOf("m1" to 10_000L, "m2" to 10_000L, "m3" to 10_000L),
                    ),
                )
            settlements.value =
                listOf(
                    settlement(from = "m2", to = "m1", amount = 10_000, status = SettlementStatus.PENDING),
                )

            val balances = getBalances("t1").first()
            assertEquals(0L, balances.sumOf { it.amount.satang })
            // PENDING must not move money: m2 still owes 100 baht.
            assertEquals(-10_000L, balances.byId("m2").amount.satang)
            assertEquals(20_000L, balances.byId("m1").amount.satang)

            settlements.value =
                listOf(
                    settlement(from = "m2", to = "m1", amount = 10_000, status = SettlementStatus.CONFIRMED),
                )
            val after = getBalances("t1").first()
            assertEquals(0L, after.byId("m2").amount.satang)
            assertEquals(10_000L, after.byId("m1").amount.satang)
        }

    @Test
    @DisplayName("Suggested transfers settle every balance to zero")
    fun transfersSettleEverything() =
        runTest {
            expenses.value =
                listOf(
                    expense(
                        total = 30_000,
                        paidBy = "m1",
                        shares = listOf("m1" to 10_000L, "m2" to 10_000L, "m3" to 10_000L),
                    ),
                    expense(total = 9_000, paidBy = "m2", shares = listOf("m1" to 4_500L, "m2" to 4_500L)),
                )
            settlements.value = emptyList()

            val balances = getBalances("t1").first()
            val transfers = getTransfers("t1").first()

            val net = balances.associate { it.memberId to it.amount.satang }.toMutableMap()
            transfers.forEach { transfer ->
                net.merge(transfer.fromMemberId, transfer.amount.satang, Long::plus)
                net.merge(transfer.toMemberId, -transfer.amount.satang, Long::plus)
            }
            assertTrue(net.values.all { it == 0L }) { "Unsettled after transfers: $net" }
            assertTrue(transfers.size <= 2)
        }

    private fun List<Balance>.byId(id: String) = first { it.memberId == id }

    private fun member(id: String) = Member(id = id, userId = id, displayName = id)

    private fun expense(
        total: Long,
        paidBy: String,
        shares: List<Pair<String, Long>>,
    ) = Expense(
        id = "e-$paidBy-$total",
        title = "bill",
        totalAmount = Money(total),
        paidByMemberId = paidBy,
        date = LocalDate(2026, 12, 5),
        splitMode = SplitMode.EXACT,
        shares = shares.map { Share(it.first, Money(it.second)) },
        createdBy = paidBy,
    )

    private fun settlement(
        from: String,
        to: String,
        amount: Long,
        status: SettlementStatus,
    ) = Settlement(
        id = "s-$from-$to",
        fromMemberId = from,
        toMemberId = to,
        amount = Money(amount),
        status = status,
        markedBy = from,
    )
}

private class FakeTripRepo(private val members: MutableStateFlow<List<Member>>) : TripRepository {
    override fun observeTrips(userId: String): Flow<List<Trip>> = flowOf(emptyList())

    override fun observeTrip(tripId: String): Flow<Trip?> = flowOf(null)

    override fun observeMembers(tripId: String): Flow<List<Member>> = members

    override suspend fun createTrip(draft: TripDraft): Result<String> = Result.success("t1")

    override suspend fun updateTrip(trip: Trip): Result<Unit> = Result.success(Unit)

    override suspend fun setArchived(
        tripId: String,
        archived: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateTripNote(
        tripId: String,
        note: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun setInviteActive(
        code: String,
        active: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getInvitePreview(code: String): Result<InvitePreview> =
        Result.success(InvitePreview("t1", "trip"))

    override suspend fun joinByCode(
        code: String,
        userId: String,
    ): Result<String> = Result.success("t1")

    override suspend fun addGuestMember(
        tripId: String,
        displayName: String,
    ): Result<String> = Result.success("m-guest")

    override suspend fun updateMember(
        tripId: String,
        member: Member,
    ): Result<Unit> = Result.success(Unit)
}

private class FakeExpenseRepo(private val expenses: MutableStateFlow<List<Expense>>) : ExpenseRepository {
    override fun observeExpenses(tripId: String): Flow<List<Expense>> = expenses

    override suspend fun upsert(
        tripId: String,
        expense: Expense,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun delete(
        tripId: String,
        expenseId: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun uploadSlip(
        tripId: String,
        expenseId: String,
        bytes: ByteArray,
    ): Result<String> = Result.success("url")
}

private class FakeSettlementRepo(private val settlements: MutableStateFlow<List<Settlement>>) : SettlementRepository {
    override fun observeSettlements(tripId: String): Flow<List<Settlement>> = settlements

    override suspend fun create(
        tripId: String,
        settlement: Settlement,
    ): Result<String> = Result.success("s1")

    override suspend fun confirm(
        tripId: String,
        settlementId: String,
        confirmedByMemberId: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun delete(
        tripId: String,
        settlementId: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun uploadSlip(
        tripId: String,
        settlementId: String,
        bytes: ByteArray,
    ): Result<String> = Result.success("url")
}
