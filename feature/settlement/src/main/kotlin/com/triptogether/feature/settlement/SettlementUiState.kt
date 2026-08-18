package com.triptogether.feature.settlement

import androidx.annotation.StringRes
import com.triptogether.core.domain.model.Balance
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Transfer

data class SettlementUiState(
    val isLoading: Boolean = true,
    val balances: List<Balance> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val members: List<Member> = emptyList(),
    val myMemberId: String? = null,
    val tripTotal: Money = Money.ZERO,
    val perPersonAverage: Money = Money.ZERO,
) {
    val pending: List<Settlement> get() = settlements.filter { it.status == SettlementStatus.PENDING }

    val confirmed: List<Settlement> get() = settlements.filter { it.status == SettlementStatus.CONFIRMED }

    val allSettled: Boolean
        get() = !isLoading && transfers.isEmpty() && pending.isEmpty() && balances.all { it.amount.isZero }

    fun memberName(memberId: String): String = members.firstOrNull { it.id == memberId }?.displayName ?: ""

    fun promptpayIdOf(memberId: String): String? = members.firstOrNull { it.id == memberId }?.promptpayId
}

sealed interface SettlementEvent {
    data class Message(
        @StringRes val messageResId: Int,
    ) : SettlementEvent
}
