package com.triptogether.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.triptogether.core.data.dto.MemberDto
import com.triptogether.core.data.dto.ROLE_MEMBER
import com.triptogether.core.data.dto.ROLE_OWNER
import com.triptogether.core.data.dto.TripDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.data.util.InviteCodes
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTripRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val auth: FirebaseAuth,
    ) : TripRepository {
        override fun observeTrips(userId: String): Flow<List<Trip>> =
            callbackFlow {
                val registration =
                    firestore.collection(TRIPS)
                        .whereArrayContains(FIELD_MEMBER_IDS, userId)
                        .orderBy(FIELD_START_DATE, Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            trySend(snapshot?.documents?.mapNotNull { it.toTrip() } ?: emptyList())
                        }
                awaitClose { registration.remove() }
            }

        override fun observeTrip(tripId: String): Flow<Trip?> =
            callbackFlow {
                val registration =
                    firestore.collection(TRIPS).document(tripId)
                        .addSnapshotListener { snapshot, _ -> trySend(snapshot?.toTrip()) }
                awaitClose { registration.remove() }
            }

        override fun observeMembers(tripId: String): Flow<List<Member>> =
            callbackFlow {
                val registration =
                    firestore.collection(TRIPS).document(tripId).collection(MEMBERS)
                        .addSnapshotListener { snapshot, _ ->
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
                val code = reserveInviteCode(tripRef.id, draft.name)

                val batch = firestore.batch()
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
                batch.set(
                    tripRef.collection(MEMBERS).document(),
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
                batch.commit().await()
                tripRef.id
            }

        override suspend fun updateTrip(trip: Trip): Result<Unit> =
            runCatching {
                // Day-doc reconciliation on date change arrives with S03 edit-trip (M2.4).
                firestore.collection(TRIPS).document(trip.id).update(
                    mapOf(
                        "name" to trip.name,
                        "coverUrl" to trip.coverUrl,
                        FIELD_START_DATE to trip.startDate.toString(),
                        "endDate" to trip.endDate.toString(),
                        FIELD_ARCHIVED to trip.archived,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                ).await()
                Unit
            }

        override suspend fun setArchived(
            tripId: String,
            archived: Boolean,
        ): Result<Unit> =
            runCatching {
                firestore.collection(TRIPS).document(tripId)
                    .update(FIELD_ARCHIVED, archived, FIELD_UPDATED_AT, FieldValue.serverTimestamp())
                    .await()
                Unit
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

                val alreadyMember =
                    !firestore.collection(TRIPS).document(tripId).collection(MEMBERS)
                        .whereEqualTo("userId", userId).limit(1).get().await().isEmpty
                if (!alreadyMember) {
                    val profile = firestore.collection(USERS).document(userId).get().await()
                    val batch = firestore.batch()
                    batch.update(
                        firestore.collection(TRIPS).document(tripId),
                        FIELD_MEMBER_IDS,
                        FieldValue.arrayUnion(userId),
                    )
                    batch.set(
                        firestore.collection(TRIPS).document(tripId).collection(MEMBERS).document(),
                        MemberDto(
                            userId = userId,
                            displayName = profile.getString("displayName") ?: "",
                            photoUrl = profile.getString("photoUrl"),
                            promptpayId = profile.getString("promptpayId"),
                            role = ROLE_MEMBER,
                        ).withJoinedAt(),
                    )
                    batch.commit().await()
                }
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
                ).await()
                memberRef.id
            }

        override suspend fun updateMember(
            tripId: String,
            member: Member,
        ): Result<Unit> =
            runCatching {
                firestore.collection(TRIPS).document(tripId).collection(MEMBERS).document(member.id)
                    .set(member.toDto(), SetOptions.merge()).await()
                Unit
            }

        /** Reserves a unique code doc in inviteCodes/, retrying on the rare collision. */
        private suspend fun reserveInviteCode(
            tripId: String,
            tripName: String,
        ): String {
            repeat(MAX_CODE_ATTEMPTS) {
                val code = InviteCodes.random()
                val ref = firestore.collection(INVITE_CODES).document(code)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "tripId" to tripId,
                            "tripName" to tripName,
                            "active" to true,
                            "expiresAt" to null,
                        ),
                    ).await()
                    return code
                }
            }
            error("Could not allocate an invite code")
        }

        private companion object {
            const val TRIPS = "trips"
            const val MEMBERS = "members"
            const val DAYS = "days"
            const val USERS = "users"
            const val INVITE_CODES = "inviteCodes"
            const val FIELD_MEMBER_IDS = "memberIds"
            const val FIELD_START_DATE = "startDate"
            const val FIELD_ARCHIVED = "archived"
            const val FIELD_UPDATED_AT = "updatedAt"
            const val MAX_CODE_ATTEMPTS = 5
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
