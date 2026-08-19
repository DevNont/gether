package com.triptogether.core.testing

import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettlementRepository]: settlements come from the given state flow, writes succeed as no-ops. */
class FakeSettlementRepo(private val settlements: MutableStateFlow<List<Settlement>>) : SettlementRepository {
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
