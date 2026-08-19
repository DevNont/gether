package com.triptogether.core.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.triptogether.core.data.awaitWrite
import com.triptogether.core.data.dto.MemberDto
import com.triptogether.core.data.dto.ROLE_MEMBER
import com.triptogether.core.data.dto.ROLE_OWNER
import com.triptogether.core.data.dto.TripDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.data.util.InviteCodes
import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.NetworkMonitor
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTripRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
        private val networkMonitor: NetworkMonitor,
    ) : TripRepository {
        override fun observeTrips(userId: String): Flow<List<Trip>> =
            callbackFlow {
                // archived filter must stay: the deployed composite index is
                // memberIds CONTAINS + archived ASC + startDate DESC; dropping it
                // makes Firestore reject the query with FAILED_PRECONDITION.
                val registration =
                    firestore.collection(TRIPS)
                        .whereArrayContains(FIELD_MEMBER_IDS, userId)
                        .whereEqualTo(FIELD_ARCHIVED, false)
                        .orderBy(FIELD_START_DATE, Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(snapshot?.documents?.mapNotNull { it.toTrip() } ?: emptyList())
                        }
                awaitClose { registration.remove() }
            }

        override fun observeTrip(tripId: String): Flow<Trip?> =
            callbackFlow {
                val registration =
                    firestore.collection(TRIPS).document(tripId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(snapshot?.toTrip())
                        }
                awaitClose { registration.remove() }
            }

        override fun observeMembers(tripId: String): Flow<List<Member>> =
            callbackFlow {
                val registration =
                    firestore.collection(TRIPS).document(tripId).collection(MEMBERS)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(
                                snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(MemberDto::class.java)?.toDomain(doc.id)
                                } ?: emptyList(),
                            )
                        }
                awaitClose { registration.remove() }
            }

        override suspend fun createTrip(draft: TripDraft): Result<String> =
            runCatching {
                val uid = auth.currentUser?.uid ?: error("Not signed in")
                val profile = firestore.collection(USERS).document(uid).get().await()
                val tripRef = firestore.collection(TRIPS).document()
                val code = reserveInviteCode()

                val batch = firestore.batch()
                // Rules require the invite doc to be minted in the same batch as its
                // trip (create checks getAfter(trip).ownerId == uid).
                batch.set(
                    firestore.collection(INVITE_CODES).document(code),
                    mapOf(
                        "tripId" to tripRef.id,
                        "tripName" to draft.name,
                        "active" to true,
                        "expiresAt" to Timestamp(Date(System.currentTimeMillis() + INVITE_TTL_MS)),
                    ),
                )
                batch.set(
                    tripRef,
                    mapOf(
                        "name" to draft.name,
                        "coverUrl" to draft.coverUrl,
                        FIELD_START_DATE to draft.startDate.toString(),
                        "endDate" to draft.endDate.toString(),
                        "ownerId" to uid,
                        FIELD_MEMBER_IDS to listOf(uid),
                        "currency" to "THB",
                        "inviteCode" to code,
                        FIELD_ARCHIVED to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                // Member doc id == auth uid for account holders, so joins are idempotent.
                batch.set(
                    tripRef.collection(MEMBERS).document(uid),
                    MemberDto(
                        userId = uid,
                        displayName = profile.getString("displayName") ?: "",
                        photoUrl = profile.getString("photoUrl"),
                        promptpayId = profile.getString("promptpayId"),
                        role = ROLE_OWNER,
                    ).withJoinedAt(),
                )
                val dayCount = draft.startDate.daysUntil(draft.endDate) + 1
                repeat(dayCount) { offset ->
                    val date = draft.startDate.plus(offset, DateTimeUnit.DAY).toString()
                    batch.set(
                        tripRef.collection(DAYS).document(date),
                        mapOf("date" to date, "note" to null),
                    )
                }
                batch.commit().awaitWrite(networkMonitor)
                tripRef.id
            }

        override suspend fun updateTrip(trip: Trip): Result<Unit> =
            runCatching {
                val ref = firestore.collection(TRIPS).document(trip.id)
                val old = ref.get().await()
                val oldDates =
                    datesBetween(
                        LocalDate.parse(checkNotNull(old.getString(FIELD_START_DATE))),
                        LocalDate.parse(checkNotNull(old.getString("endDate"))),
                    )
                val newDates = datesBetween(trip.startDate, trip.endDate)

                // Shrinking the range deletes those days together with their activities;
                // the UI has already confirmed this with the user (S05 spec).
                for (date in oldDates - newDates) {
                    val dayRef = ref.collection(DAYS).document(date.toString())
                    val activities = dayRef.collection(ACTIVITIES).get().await()
                    val batch = firestore.batch()
                    activities.documents.forEach { batch.delete(it.reference) }
                    batch.delete(dayRef)
                    batch.commit().awaitWrite(networkMonitor)
                }

                val batch = firestore.batch()
                batch.update(
                    ref,
                    mapOf(
                        "name" to trip.name,
                        "coverUrl" to trip.coverUrl,
                        FIELD_START_DATE to trip.startDate.toString(),
                        "endDate" to trip.endDate.toString(),
                        FIELD_ARCHIVED to trip.archived,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                // Growing the range only adds fresh day docs; existing ones stay untouched.
                (newDates - oldDates).forEach { date ->
                    batch.set(
                        ref.collection(DAYS).document(date.toString()),
                        mapOf("date" to date.toString(), "note" to null),
                    )
                }
                batch.commit().awaitWrite(networkMonitor)
                Unit
            }

        /** Client-side cascade; replaced by the onTripDelete Cloud Function once Blaze is on. */
        override suspend fun deleteTrip(tripId: String): Result<Unit> =
            runCatching {
                val ref = firestore.collection(TRIPS).document(tripId)
                val inviteCode = ref.get().await().getString("inviteCode")

                val days = ref.collection(DAYS).get().await()
                for (day in days.documents) {
                    val activities = day.reference.collection(ACTIVITIES).get().await()
                    val batch = firestore.batch()
                    activities.documents.forEach { batch.delete(it.reference) }
                    batch.delete(day.reference)
                    batch.commit().awaitWrite(networkMonitor)
                }
                for (collection in listOf(MEMBERS, "expenses", "settlements", "checklist", "polls", "meetups")) {
                    val docs = ref.collection(collection).get().await()
                    if (docs.isEmpty) continue
                    val batch = firestore.batch()
                    docs.documents.forEach { batch.delete(it.reference) }
                    batch.commit().awaitWrite(networkMonitor)
                }
                inviteCode?.let {
                    firestore.collection(INVITE_CODES).document(it).delete().awaitWrite(networkMonitor)
                }
                // The trip doc goes last — subcollection rules need it alive to prove membership.
                ref.delete().awaitWrite(networkMonitor)
                Unit
            }

        private fun datesBetween(
            start: LocalDate,
            end: LocalDate,
        ): Set<LocalDate> = (0..start.daysUntil(end)).map { start.plus(it, DateTimeUnit.DAY) }.toSet()

        override suspend fun setArchived(
            tripId: String,
            archived: Boolean,
        ): Result<Unit> =
            runCatching {
                firestore.collection(TRIPS).document(tripId)
                    .update(FIELD_ARCHIVED, archived, FIELD_UPDATED_AT, FieldValue.serverTimestamp())
                    .awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun updateTripNote(
            tripId: String,
            note: String?,
        ): Result<Unit> =
            runCatching {
                firestore.collection(TRIPS).document(tripId)
                    .update("note", note, FIELD_UPDATED_AT, FieldValue.serverTimestamp())
                    .awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun setInviteActive(
            code: String,
            active: Boolean,
        ): Result<Unit> =
            runCatching {
                firestore.collection(INVITE_CODES).document(code.uppercase())
                    .update("active", active).awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun getInvitePreview(code: String): Result<InvitePreview> =
            runCatching {
                val snapshot =
                    firestore.collection(INVITE_CODES).document(code.uppercase()).get().await()
                check(snapshot.exists() && snapshot.getBoolean("active") == true) { "Invalid invite code" }
                InvitePreview(
                    tripId = checkNotNull(snapshot.getString("tripId")) { "Corrupt invite code" },
                    tripName = snapshot.getString("tripName") ?: "",
                )
            }

        override suspend fun joinByCode(
            code: String,
            userId: String,
        ): Result<String> =
            runCatching {
                val codeSnap =
                    firestore.collection(INVITE_CODES).document(code.uppercase()).get().await()
                check(codeSnap.exists() && codeSnap.getBoolean("active") == true) { "Invalid invite code" }
                val tripId = checkNotNull(codeSnap.getString("tripId")) { "Corrupt invite code" }

                // A non-member cannot query the members collection (rules), so joining
                // is made idempotent instead: the member doc id is the auth uid —
                // arrayUnion + set on a fixed id make a repeat join a no-op.
                val profile = firestore.collection(USERS).document(userId).get().await()
                val batch = firestore.batch()
                batch.update(
                    firestore.collection(TRIPS).document(tripId),
                    FIELD_MEMBER_IDS,
                    FieldValue.arrayUnion(userId),
                )
                batch.set(
                    firestore.collection(TRIPS).document(tripId).collection(MEMBERS).document(userId),
                    MemberDto(
                        userId = userId,
                        displayName = profile.getString("displayName") ?: "",
                        photoUrl = profile.getString("photoUrl"),
                        promptpayId = profile.getString("promptpayId"),
                        role = ROLE_MEMBER,
                    ).withJoinedAt(),
                    SetOptions.merge(),
                )
                batch.commit().awaitWrite(networkMonitor)
                tripId
            }

        override suspend fun addGuestMember(
            tripId: String,
            displayName: String,
        ): Result<String> =
            runCatching {
                val memberRef = firestore.collection(TRIPS).document(tripId).collection(MEMBERS).document()
                memberRef.set(
                    MemberDto(userId = null, displayName = displayName, role = ROLE_MEMBER).withJoinedAt(),
                ).awaitWrite(networkMonitor)
                memberRef.id
            }

        override suspend fun updateMember(
            tripId: String,
            member: Member,
        ): Result<Unit> =
            runCatching {
                firestore.collection(TRIPS).document(tripId).collection(MEMBERS).document(member.id)
                    .set(member.toDto(), SetOptions.merge()).awaitWrite(networkMonitor)
                Unit
            }

        /**
         * Picks a code not currently in use. The doc itself is written by the caller
         * inside the trip-create batch (rules verify ownership via getAfter), so the
         * tiny window between this check and the commit is an accepted race.
         */
        private suspend fun reserveInviteCode(): String {
            repeat(MAX_CODE_ATTEMPTS) {
                val code = InviteCodes.random()
                val ref = firestore.collection(INVITE_CODES).document(code)
                if (!ref.get().await().exists()) return code
            }
            error("Could not allocate an invite code")
        }

        private companion object {
            const val TRIPS = "trips"
            const val MEMBERS = "members"
            const val DAYS = "days"
            const val ACTIVITIES = "activities"
            const val USERS = "users"
            const val INVITE_CODES = "inviteCodes"
            const val FIELD_MEMBER_IDS = "memberIds"
            const val FIELD_START_DATE = "startDate"
            const val FIELD_ARCHIVED = "archived"
            const val FIELD_UPDATED_AT = "updatedAt"
            const val MAX_CODE_ATTEMPTS = 5

            /** Invite codes stop working one week after the trip is created. */
            const val INVITE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        }
    }

private fun DocumentSnapshot.toTrip(): Trip? = toObject(TripDto::class.java)?.toDomain(id)

private fun MemberDto.withJoinedAt(): Map<String, Any?> =
    mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "promptpayId" to promptpayId,
        "role" to role,
        "joinedAt" to FieldValue.serverTimestamp(),
    )
