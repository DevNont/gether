package com.triptogether.core.testing

import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ExpenseRepository]: expenses come from the given state flow, writes succeed as no-ops. */
class FakeExpenseRepo(private val expenses: MutableStateFlow<List<Expense>>) : ExpenseRepository {
    override fun observeExpenses(tripId: String): Flow<List<Expense>> = expenses

    override suspend fun upsert(
        tripId: String,
        expense: Expense,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateOwnShare(
        tripId: String,
        expenseId: String,
        memberId: String,
        amount: Money,
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
