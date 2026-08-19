package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits the signed-in user's profile, or null when signed out. */
    fun observeAuthState(): Flow<User?>

    /** Exchanges a Google ID token (obtained via Credential Manager in the app layer) for a session. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<User>

    suspend fun signOut(): Result<Unit>
}
