package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(tripId: String): Flow<List<Expense>>

    /** Rejects any expense whose shares do not sum to the total (see Expense.isBalanced). */
    suspend fun upsert(
        tripId: String,
        expense: Expense,
    ): Result<Unit>

    suspend fun delete(
        tripId: String,
        expenseId: String,
    ): Result<Unit>

    /** Uploads a payment slip photo and returns its download URL. */
    suspend fun uploadSlip(
        tripId: String,
        expenseId: String,
        bytes: ByteArray,
    ): Result<String>
}
