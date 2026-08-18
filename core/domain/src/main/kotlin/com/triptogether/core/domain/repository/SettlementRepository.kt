package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    fun observeSettlements(tripId: String): Flow<List<Settlement>>

    /** Records a transfer marked as paid (status PENDING). Returns the new settlementId. */
    suspend fun create(
        tripId: String,
        settlement: Settlement,
    ): Result<String>

    /** Receiver confirms the transfer; only CONFIRMED settlements count toward balances. */
    suspend fun confirm(
        tripId: String,
        settlementId: String,
        confirmedByMemberId: String,
    ): Result<Unit>

    suspend fun delete(
        tripId: String,
        settlementId: String,
    ): Result<Unit>

    /** Uploads a transfer slip photo and returns its download URL. */
    suspend fun uploadSlip(
        tripId: String,
        settlementId: String,
        bytes: ByteArray,
    ): Result<String>
}
