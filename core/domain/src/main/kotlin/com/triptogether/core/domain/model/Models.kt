package com.triptogether.core.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Pure domain models. Nothing here may reference Android or Firebase types —
 * this package is the part that moves to Kotlin Multiplatform when iOS starts.
 */

data class Trip(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val ownerId: String,
    val inviteCode: String,
    val archived: Boolean = false,
    val note: String? = null,
) {
    /** Inclusive of both the start and the end date, so a same-day trip is 1. */
    val dayCount: Int get() = startDate.daysUntil(endDate) + 1

    val dates: List<LocalDate>
        get() = (0 until dayCount).map { startDate.plus(it, DateTimeUnit.DAY) }
}

data class Member(
    val id: String,
    val userId: String?,
    val displayName: String,
    val photoUrl: String? = null,
    val promptpayId: String? = null,
    val role: MemberRole = MemberRole.MEMBER,
) {
    val isGuest: Boolean get() = userId == null
}

enum class MemberRole { OWNER, MEMBER }

data class DayPlan(
    val id: String,
    val date: LocalDate,
    val note: String? = null,
    val activities: List<Activity> = emptyList(),
)

/** What a plan entry is: a place to visit, a restaurant to eat at, or the day's stay. */
enum class ActivityType { PLACE, FOOD, STAY }

data class Activity(
    val id: String,
    val title: String,
    val type: ActivityType = ActivityType.PLACE,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val placeName: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val note: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val sortOrder: Int = 0,
    val createdBy: String,
)

data class Attachment(val name: String, val url: String, val mimeType: String)

enum class ExpenseCategory { FOOD, STAY, TRANSPORT, ACTIVITY, OTHER }

enum class SplitMode { EQUAL, EXACT, SHARES }

data class Share(
    val memberId: String,
    val amount: Money,
    val weight: Int? = null,
)

data class Expense(
    val id: String,
    val title: String,
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val totalAmount: Money,
    val paidByMemberId: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val splitMode: SplitMode,
    val shares: List<Share>,
    val slipUrls: List<String> = emptyList(),
    val note: String? = null,
    val createdBy: String,
) {
    /** Must always hold. Enforced on save and asserted in tests. */
    val isBalanced: Boolean get() = shares.sumOf { it.amount.satang } == totalAmount.satang

    fun shareOf(memberId: String): Money = shares.firstOrNull { it.memberId == memberId }?.amount ?: Money.ZERO
}

enum class SettlementStatus { PENDING, CONFIRMED }

data class Settlement(
    val id: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Money,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val markedBy: String,
    val confirmedBy: String? = null,
    val slipUrl: String? = null,
)

/** A member's net position in a trip. Positive = others owe them. */
data class Balance(val memberId: String, val amount: Money)

/** One suggested transfer produced by [DebtSimplifier]. */
data class Transfer(
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Money,
)

enum class ChecklistScope { PERSONAL, SHARED }

data class ChecklistItem(
    val id: String,
    val title: String,
    val scope: ChecklistScope,
    val assignedMemberId: String? = null,
    val checkedBy: Set<String> = emptySet(),
    val sortOrder: Int = 0,
)

/** A shared trip appointment (e.g. "meet at the lobby 07:00") with a local reminder. */
data class Meetup(
    val id: String,
    val title: String,
    val place: String? = null,
    val date: LocalDate,
    val time: LocalTime,
    /** Minutes before [time] to fire the reminder; 0 = at the appointed time. */
    val reminderMinutesBefore: Int = 30,
    val note: String? = null,
    val createdBy: String,
)

/** Profile stored at users/{userId}; id is the auth UID. */
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String? = null,
    val promptpayId: String? = null,
)

const val INVITE_CODE_LENGTH = 6

/** What a prospective member sees before confirming a join (from inviteCodes/{code}). */
data class InvitePreview(
    val tripId: String,
    val tripName: String,
)

/** Input for creating a trip; server assigns id and invite code. */
data class TripDraft(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val coverUrl: String? = null,
)

data class Poll(
    val id: String,
    val question: String,
    val options: List<PollOption>,
    val multiChoice: Boolean = false,
    val closed: Boolean = false,
    /** Optional link to an itinerary activity (S13). */
    val activityId: String? = null,
    val createdBy: String = "",
)

data class PollOption(
    val id: String,
    val label: String,
    val voterMemberIds: Set<String> = emptySet(),
)
