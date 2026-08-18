package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(userId: String): Flow<User?>

    suspend fun updateProfile(user: User): Result<Unit>

    /** Adds this device's push token to users/{userId}.fcmTokens. */
    suspend fun registerPushToken(
        userId: String,
        token: String,
    ): Result<Unit>
}
