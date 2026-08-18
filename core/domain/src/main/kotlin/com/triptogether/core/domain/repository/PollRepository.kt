package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Poll
import kotlinx.coroutines.flow.Flow

interface PollRepository {
    fun observePolls(tripId: String): Flow<List<Poll>>

    suspend fun create(
        tripId: String,
        poll: Poll,
    ): Result<String>

    suspend fun setVote(
        tripId: String,
        pollId: String,
        optionId: String,
        memberId: String,
        selected: Boolean,
    ): Result<Unit>

    suspend fun close(
        tripId: String,
        pollId: String,
    ): Result<Unit>

    suspend fun delete(
        tripId: String,
        pollId: String,
    ): Result<Unit>
}
