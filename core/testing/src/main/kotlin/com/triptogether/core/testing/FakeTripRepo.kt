package com.triptogether.core.testing

import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.domain.model.TripDraft
import com.triptogether.core.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** In-memory [TripRepository]: members come from the given state flow, writes succeed as no-ops. */
class FakeTripRepo(private val members: MutableStateFlow<List<Member>>) : TripRepository {
    override fun observeTrips(userId: String): Flow<List<Trip>> = flowOf(emptyList())

    override fun observeTrip(tripId: String): Flow<Trip?> = flowOf(null)

    override fun observeMembers(tripId: String): Flow<List<Member>> = members

    override suspend fun createTrip(draft: TripDraft): Result<String> = Result.success("t1")

    override suspend fun updateTrip(trip: Trip): Result<Unit> = Result.success(Unit)

    override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)

    override suspend fun setArchived(
        tripId: String,
        archived: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateTripNote(
        tripId: String,
        note: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun setInviteActive(
        code: String,
        active: Boolean,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun getInvitePreview(code: String): Result<InvitePreview> =
        Result.success(InvitePreview("t1", "trip"))

    override suspend fun joinByCode(
        code: String,
        userId: String,
    ): Result<String> = Result.success("t1")

    override suspend fun addGuestMember(
        tripId: String,
        displayName: String,
    ): Result<String> = Result.success("m-guest")

    override suspend fun updateMember(
        tripId: String,
        member: Member,
    ): Result<Unit> = Result.success(Unit)
}
