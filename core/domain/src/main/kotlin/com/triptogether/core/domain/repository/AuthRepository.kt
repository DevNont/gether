package com.triptogether.core.domain.repository

import com.triptogether.core.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Marker for the UI host a browser-based sign-in flow launches from.
 * The app's activity implements it; the data layer downcasts. Keeps the
 * domain free of Android types.
 */
interface AuthUiHost

interface AuthRepository {
    /** Emits the signed-in user's profile, or null when signed out. */
    fun observeAuthState(): Flow<User?>

    /** LINE Login via the oidc.line provider (browser flow launched from [host]). */
    suspend fun signInWithLine(host: AuthUiHost): Result<User>

    /** Device-local account with a user-entered display name; upgradable later via [linkWithLine]. */
    suspend fun signInAnonymously(displayName: String): Result<User>

    /** Upgrades the current anonymous account to LINE, keeping the same uid (trips stay attached). */
    suspend fun linkWithLine(host: AuthUiHost): Result<User>

    suspend fun signOut(): Result<Unit>
}
