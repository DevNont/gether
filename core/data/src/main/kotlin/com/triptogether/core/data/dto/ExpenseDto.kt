package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import kotlinx.datetime.LocalDate

/**
 * Firestore shape of trips/{tripId}/expenses/{expenseId}.
 * Money is ALWAYS a Long in satang (docs/02); category is lowercase, splitMode uppercase.
 */
data class ExpenseDto(
    val title: String = "",
    val category: String = "food",
    val totalAmount: Long = 0,
    val paidByMemberId: String = "",
    val date: String = "",
    val splitMode: String = "EQUAL",
    val shares: List<ShareDto> = emptyList(),
    val slipUrls: List<String> = emptyList(),
    val note: String? = null,
    val createdBy: String = "",
)

data class ShareDto(
    val memberId: String = "",
    val amount: Long = 0,
    val weight: Int? = null,
)

fun ExpenseDto.toDomain(id: String): Expense =
    Expense(
        id = id,
        title = title,
        category = category.toCategory(),
        totalAmount = Money(totalAmount),
        paidByMemberId = paidByMemberId,
        date = LocalDate.parse(date),
        splitMode = runCatching { SplitMode.valueOf(splitMode) }.getOrDefault(SplitMode.EQUAL),
        shares = shares.map { Share(memberId = it.memberId, amount = Money(it.amount), weight = it.weight) },
        slipUrls = slipUrls,
        note = note,
        createdBy = createdBy,
    )

fun Expense.toDto(): ExpenseDto =
    ExpenseDto(
        title = title,
        category = category.toWireName(),
        totalAmount = totalAmount.satang,
        paidByMemberId = paidByMemberId,
        date = date.toString(),
        splitMode = splitMode.name,
        shares = shares.map { ShareDto(memberId = it.memberId, amount = it.amount.satang, weight = it.weight) },
        slipUrls = slipUrls,
        note = note,
        createdBy = createdBy,
    )

fun ExpenseCategory.toWireName(): String = name.lowercase()

private fun String.toCategory(): ExpenseCategory =
    runCatching { ExpenseCategory.valueOf(uppercase()) }.getOrDefault(ExpenseCategory.OTHER)
