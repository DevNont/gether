package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Money
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(tripId: String): Flow<List<Expense>>

    /** Rejects any expense whose shares do not sum to the total (see Expense.isBalanced). */
    suspend fun upsert(
        tripId: String,
        expense: Expense,
    ): Result<Unit>

    /**
     * ITEMIZED mode only: set just this member's own share and recompute the total
     * (= sum of all shares) atomically, without disturbing other members' shares.
     */
    suspend fun updateOwnShare(
        tripId: String,
        expenseId: String,
        memberId: String,
        amount: Money,
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
