package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

interface ChecklistRepository {
    fun observeItems(tripId: String): Flow<List<ChecklistItem>>

    suspend fun upsert(
        tripId: String,
        item: ChecklistItem,
    ): Result<Unit>

    suspend fun delete(
        tripId: String,
        itemId: String,
    ): Result<Unit>

    suspend fun setChecked(
        tripId: String,
        itemId: String,
        memberId: String,
        checked: Boolean,
    ): Result<Unit>
}
