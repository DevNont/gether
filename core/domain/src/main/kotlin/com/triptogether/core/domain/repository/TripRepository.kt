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

    suspend fun updateTrip(trip: Trip): Result<Unit>

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
