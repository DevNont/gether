package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus

/** Firestore shape of trips/{tripId}/settlements/{settlementId}. Amount is Long satang. */
data class SettlementDto(
    val fromMemberId: String = "",
    val toMemberId: String = "",
    val amount: Long = 0,
    val status: String = "PENDING",
    val markedBy: String = "",
    val confirmedBy: String? = null,
    val slipUrl: String? = null,
)

fun SettlementDto.toDomain(id: String): Settlement =
    Settlement(
        id = id,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amount = Money(amount),
        status = runCatching { SettlementStatus.valueOf(status) }.getOrDefault(SettlementStatus.PENDING),
        markedBy = markedBy,
        confirmedBy = confirmedBy,
        slipUrl = slipUrl,
    )

fun Settlement.toDto(): SettlementDto =
    SettlementDto(
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amount = amount.satang,
        status = status.name,
        markedBy = markedBy,
        confirmedBy = confirmedBy,
        slipUrl = slipUrl,
    )
