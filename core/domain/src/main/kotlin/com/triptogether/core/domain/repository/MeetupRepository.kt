package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Meetup
import kotlinx.coroutines.flow.Flow

interface MeetupRepository {
    fun observeMeetups(tripId: String): Flow<List<Meetup>>

    suspend fun upsert(
        tripId: String,
        meetup: Meetup,
    ): Result<Unit>

    suspend fun delete(
        tripId: String,
        meetupId: String,
    ): Result<Unit>
}
