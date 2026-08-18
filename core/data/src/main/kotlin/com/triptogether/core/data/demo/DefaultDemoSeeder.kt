package com.triptogether.core.data.demo

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.ActivityType
import com.triptogether.core.domain.model.ChecklistItem
import com.triptogether.core.domain.model.ChecklistScope
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Poll
import com.triptogether.core.domain.model.PollOption
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.money.ExpenseSplitter
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.ChecklistRepository
import com.triptogether.core.domain.repository.DemoSeeder
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.PlanRepository
import com.triptogether.core.domain.repository.PollRepository
import com.triptogether.core.domain.repository.SettlementRepository
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug helper: seeds one fully-populated demo trip (3-day Japan) so every screen has
 * realistic content to look at. Writes through the normal repositories, so security
 * rules and the money invariants apply exactly as in production.
 */
@Singleton
class DefaultDemoSeeder
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tripRepository: TripRepository,
        private val planRepository: PlanRepository,
        private val expenseRepository: ExpenseRepository,
        private val settlementRepository: SettlementRepository,
        private val checklistRepository: ChecklistRepository,
        private val pollRepository: PollRepository,
    ) : DemoSeeder {
        private companion object {
            const val TAG = "DemoSeeder"
        }

        override suspend fun seedJapanTrip(): Result<String> =
            runCatching {
                val uid = authRepository.observeAuthState().filterNotNull().first().id
                android.util.Log.i(TAG, "seeding for uid=$uid")
                val start = Clock.System.todayIn(TimeZone.currentSystemDefault())
                // A flight booked a month ahead — lands in the "paid in advance" group.
                val prepaidDate = start.plus(-30, DateTimeUnit.DAY).toString()
                val d1 = start.toString()
                val d2 = start.plus(1, DateTimeUnit.DAY).toString()
                val d3 = start.plus(2, DateTimeUnit.DAY).toString()

                val tripId =
                    tripRepository.createTrip(
                        TripDraft(name = "ทริปญี่ปุ่น", startDate = start, endDate = start.plus(2, DateTimeUnit.DAY)),
                    ).getOrThrow()

                // The members listener emits an empty snapshot before the owner doc lands;
                // wait for the emission that actually contains us.
                val ownerId =
                    tripRepository.observeMembers(tripId)
                        .first { members -> members.any { it.userId == uid } }
                        .first { it.userId == uid }.id
                val friendId = tripRepository.addGuestMember(tripId, "สมหญิง").getOrThrow()
                val catId = tripRepository.addGuestMember(tripId, "น้องแมว").getOrThrow()
                val everyone = listOf(ownerId, friendId, catId)

                tripRepository.updateTripNote(tripId, "เที่ยวโตเกียว 3 วัน กินให้พุงกาง 🍜")

                seedDay1(tripId, ownerId, d1)
                seedDay2(tripId, ownerId, d2)
                seedDay3(tripId, ownerId, d3)
                seedExpenses(tripId, ownerId, friendId, catId, everyone, prepaidDate, d1, d2, d3)
                seedChecklist(tripId, ownerId, friendId)
                seedPoll(tripId, ownerId)
                seedSettlement(tripId, friendId, ownerId)

                android.util.Log.i(TAG, "seeded trip=$tripId")
                tripId
            }.onFailure { android.util.Log.e(TAG, "seed failed", it) }

        private suspend fun seedDay1(
            tripId: String,
            creator: String,
            day: String,
        ) {
            planRepository.upsertActivity(tripId, day, stay(creator, "โรงแรมชินจูกุ", "Shinjuku Granbell Hotel"))
            planRepository.upsertActivity(
                tripId,
                day,
                place(creator, "วัดเซ็นโซจิ", "Senso-ji Temple", LocalTime(10, 0), sort = 0),
            )
            planRepository.upsertActivity(
                tripId,
                day,
                food(creator, "ราเมงอิจิรัน", "Ichiran Shibuya", LocalTime(12, 30), sort = 1),
            )
            planRepository.upsertActivity(
                tripId,
                day,
                place(creator, "ชิบูย่าครอสซิ่ง", "Shibuya Crossing", LocalTime(15, 0), sort = 2),
            )
        }

        private suspend fun seedDay2(
            tripId: String,
            creator: String,
            day: String,
        ) {
            planRepository.upsertActivity(tripId, day, stay(creator, "โรงแรมชินจูกุ", "Shinjuku Granbell Hotel"))
            planRepository.upsertActivity(
                tripId,
                day,
                place(creator, "ดิสนีย์แลนด์", "Tokyo Disneyland", LocalTime(9, 0), sort = 0),
            )
            planRepository.upsertActivity(
                tripId,
                day,
                food(creator, "บุฟเฟต์ในสวน", "Grand Emporium", LocalTime(18, 0), sort = 1),
            )
        }

        private suspend fun seedDay3(
            tripId: String,
            creator: String,
            day: String,
        ) {
            planRepository.upsertActivity(
                tripId,
                day,
                place(creator, "ตลาดปลาสึกิจิ", "Tsukiji Outer Market", LocalTime(8, 0), sort = 0),
            )
            planRepository.upsertActivity(
                tripId,
                day,
                food(creator, "ซูชิเช้าตรู่", "Sushi Dai", LocalTime(9, 0), sort = 1),
            )
            planRepository.upsertActivity(
                tripId,
                day,
                place(creator, "ช้อปย่านกินซ่า", "Ginza", LocalTime(13, 0), sort = 2),
            )
        }

        // Four demo bills, one per split mode — long but flat and self-descriptive.
        @Suppress("LongParameterList", "LongMethod")
        private suspend fun seedExpenses(
            tripId: String,
            owner: String,
            friend: String,
            cat: String,
            everyone: List<String>,
            prepaidDate: String,
            d1: String,
            d2: String,
            d3: String,
        ) {
            // PREPAID — flight booked a month before the trip; shows in "paid in advance".
            expenseRepository.upsert(
                tripId,
                Expense(
                    id = "",
                    title = "ตั๋วเครื่องบิน ไป-กลับ",
                    category = ExpenseCategory.TRANSPORT,
                    totalAmount = Money.fromBaht(45_000),
                    paidByMemberId = owner,
                    date = parseDate(prepaidDate),
                    splitMode = SplitMode.EQUAL,
                    shares = ExpenseSplitter.splitEqually(Money.fromBaht(45_000), everyone),
                    createdBy = owner,
                ),
            )
            // EQUAL — dinner split three ways.
            expenseRepository.upsert(
                tripId,
                Expense(
                    id = "",
                    title = "มื้อเย็น — ราเมง",
                    category = ExpenseCategory.FOOD,
                    totalAmount = Money.fromBaht(1_200),
                    paidByMemberId = owner,
                    date = parseDate(d1),
                    splitMode = SplitMode.EQUAL,
                    shares = ExpenseSplitter.splitEqually(Money.fromBaht(1_200), everyone),
                    createdBy = owner,
                ),
            )
            // EXACT — hotel, owner covers more.
            expenseRepository.upsert(
                tripId,
                Expense(
                    id = "",
                    title = "ค่าโรงแรม 2 คืน",
                    category = ExpenseCategory.STAY,
                    totalAmount = Money.fromBaht(9_000),
                    paidByMemberId = owner,
                    date = parseDate(d1),
                    splitMode = SplitMode.EXACT,
                    shares =
                        listOf(
                            Share(owner, Money.fromBaht(3_000)),
                            Share(friend, Money.fromBaht(3_000)),
                            Share(cat, Money.fromBaht(3_000)),
                        ),
                    createdBy = owner,
                ),
            )
            // SHARES — Disneyland tickets, cat is a child (weight 1 vs 2).
            expenseRepository.upsert(
                tripId,
                Expense(
                    id = "",
                    title = "ตั๋วดิสนีย์แลนด์",
                    category = ExpenseCategory.ACTIVITY,
                    totalAmount = Money.fromBaht(7_500),
                    paidByMemberId = friend,
                    date = parseDate(d2),
                    splitMode = SplitMode.SHARES,
                    shares =
                        ExpenseSplitter.splitByWeights(
                            Money.fromBaht(7_500),
                            mapOf(owner to 2, friend to 2, cat to 1),
                        ),
                    createdBy = friend,
                ),
            )
            // EQUAL — transport.
            expenseRepository.upsert(
                tripId,
                Expense(
                    id = "",
                    title = "ค่ารถไฟ JR Pass",
                    category = ExpenseCategory.TRANSPORT,
                    totalAmount = Money.fromBaht(3_000),
                    paidByMemberId = cat,
                    date = parseDate(d3),
                    splitMode = SplitMode.EQUAL,
                    shares = ExpenseSplitter.splitEqually(Money.fromBaht(3_000), everyone),
                    createdBy = cat,
                ),
            )
        }

        private suspend fun seedChecklist(
            tripId: String,
            owner: String,
            friend: String,
        ) {
            listOf(
                ChecklistItem(
                    id = "",
                    title = "ทำวีซ่า",
                    scope = ChecklistScope.SHARED,
                    assignedMemberId = owner,
                    sortOrder = 0,
                ),
                ChecklistItem(
                    id = "",
                    title = "แลกเงินเยน",
                    scope = ChecklistScope.SHARED,
                    assignedMemberId = friend,
                    sortOrder = 1,
                ),
                ChecklistItem(id = "", title = "ซื้อ pocket wifi", scope = ChecklistScope.SHARED, sortOrder = 2),
                ChecklistItem(id = "", title = "เตรียมยาประจำตัว", scope = ChecklistScope.PERSONAL, sortOrder = 0),
                ChecklistItem(id = "", title = "ชาร์จ power bank", scope = ChecklistScope.PERSONAL, sortOrder = 1),
            ).forEach { checklistRepository.upsert(tripId, it) }
        }

        private suspend fun seedPoll(
            tripId: String,
            owner: String,
        ) {
            pollRepository.create(
                tripId,
                Poll(
                    id = "",
                    question = "มื้อเย็นวันสุดท้ายกินอะไรดี?",
                    options =
                        listOf(
                            PollOption(id = UUID.randomUUID().toString(), label = "ยากินิกุ"),
                            PollOption(id = UUID.randomUUID().toString(), label = "ชาบู"),
                            PollOption(id = UUID.randomUUID().toString(), label = "ซูชิ"),
                        ),
                    multiChoice = false,
                    createdBy = owner,
                ),
            )
        }

        private suspend fun seedSettlement(
            tripId: String,
            from: String,
            to: String,
        ) {
            settlementRepository.create(
                tripId,
                Settlement(
                    id = "",
                    fromMemberId = from,
                    toMemberId = to,
                    amount = Money.fromBaht(500),
                    status = SettlementStatus.PENDING,
                    markedBy = from,
                ),
            )
        }

        private fun stay(
            creator: String,
            title: String,
            place: String,
        ) = Activity(id = "", title = title, type = ActivityType.STAY, placeName = place, createdBy = creator)

        private fun place(
            creator: String,
            title: String,
            place: String,
            time: LocalTime,
            sort: Int,
        ) = Activity(
            id = "",
            title = title,
            type = ActivityType.PLACE,
            startTime = time,
            placeName = place,
            sortOrder = sort,
            createdBy = creator,
        )

        private fun food(
            creator: String,
            title: String,
            place: String,
            time: LocalTime,
            sort: Int,
        ) = Activity(
            id = "",
            title = title,
            type = ActivityType.FOOD,
            startTime = time,
            placeName = place,
            sortOrder = sort,
            createdBy = creator,
        )
    }

private fun parseDate(value: String): kotlinx.datetime.LocalDate = kotlinx.datetime.LocalDate.parse(value)
