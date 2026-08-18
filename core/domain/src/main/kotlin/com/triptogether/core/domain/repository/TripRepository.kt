package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(userId: String): Flow<List<Trip>>

    fun observeTrip(tripId: String): Flow<Trip?>

    fun observeMembers(tripId: String): Flow<List<Member>>

    /** Creates the trip, its member doc for the owner, and one day doc per date. Returns the new tripId. */
    suspend fun createTrip(draft: TripDraft): Result<String>

    /**
     * Updates trip fields and reconciles day docs (S05 spec): dates added to the
     * range get new day docs, dates removed lose their day docs AND activities —
     * callers must confirm with the user before shrinking the range.
     */
    suspend fun updateTrip(trip: Trip): Result<Unit>

    /** Owner-only. Cascades over subcollections client-side until Cloud Functions deploy. */
    suspend fun deleteTrip(tripId: String): Result<Unit>

    suspend fun setArchived(
        tripId: String,
        archived: Boolean,
    ): Result<Unit>

    /** Shared trip note on S05, editable by every member. */
    suspend fun updateTripNote(
        tripId: String,
        note: String?,
    ): Result<Unit>

    /** Owner can deactivate the invite code so new members cannot join. */
    suspend fun setInviteActive(
        code: String,
        active: Boolean,
    ): Result<Unit>

    /** Looks up an invite code for the confirm-before-join preview. Fails when unknown or inactive. */
    suspend fun getInvitePreview(code: String): Result<InvitePreview>

    /** Joins via invite code. Returns the tripId on success. */
    suspend fun joinByCode(
        code: String,
        userId: String,
    ): Result<String>

    /** Adds a member without an account (userId = null). Returns the new memberId. */
    suspend fun addGuestMember(
        tripId: String,
        displayName: String,
    ): Result<String>

    suspend fun updateMember(
        tripId: String,
        member: Member,
    ): Result<Unit>
}
