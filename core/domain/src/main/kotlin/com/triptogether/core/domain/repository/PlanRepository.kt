package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.DayPlan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun observeDayPlans(tripId: String): Flow<List<DayPlan>>

    fun observeDayPlan(
        tripId: String,
        dayId: String,
    ): Flow<DayPlan?>

    suspend fun updateDayNote(
        tripId: String,
        dayId: String,
        note: String?,
    ): Result<Unit>

    suspend fun upsertActivity(
        tripId: String,
        dayId: String,
        activity: Activity,
    ): Result<Unit>

    suspend fun deleteActivity(
        tripId: String,
        dayId: String,
        activityId: String,
    ): Result<Unit>

    /** Uploads a ticket/booking file and returns its download URL. */
    suspend fun uploadAttachment(
        tripId: String,
        activityId: String,
        fileName: String,
        bytes: ByteArray,
    ): Result<String>
}
