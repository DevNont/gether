package com.triptogether.core.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.AuthUiHost
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : AuthRepository {
        override fun observeAuthState(): Flow<User?> =
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toDomainUser()) }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }

        /**
         * LINE Login through Firebase's OIDC provider "oidc.line" (configured in the
         * Auth console with LINE's issuer https://access.line.me). Browser flow, so it
         * works without Play Services and without a custom-token backend.
         */
        override suspend fun signInWithLine(host: AuthUiHost): Result<User> =
            runCatching {
                val activity = host as? Activity ?: error("AuthUiHost must be an Activity")
                val result =
                    auth.pendingAuthResult?.await()
                        ?: auth.startActivityForSignInWithProvider(activity, lineProvider()).await()
                val firebaseUser = result.user ?: error("Sign-in returned no user")
                ensureUserDoc(firebaseUser)
                firebaseUser.toDomainUser()
            }

        /** Anonymous session with a user-entered display name (device-local until linked). */
        override suspend fun signInAnonymously(displayName: String): Result<User> =
            runCatching {
                val firebaseUser =
                    auth.signInAnonymously().await().user ?: error("Sign-in returned no user")
                firebaseUser.updateProfile(
                    userProfileChangeRequest { this.displayName = displayName },
                ).await()
                ensureUserDoc(firebaseUser, displayNameOverride = displayName)
                firebaseUser.toDomainUser().copy(displayName = displayName)
            }

        /** Links the current anonymous account to LINE — same uid, so trips stay attached. */
        override suspend fun linkWithLine(host: AuthUiHost): Result<User> =
            runCatching {
                val activity = host as? Activity ?: error("AuthUiHost must be an Activity")
                val current = auth.currentUser ?: error("Not signed in")
                val result = current.startActivityForLinkWithProvider(activity, lineProvider()).await()
                val firebaseUser = result.user ?: error("Link returned no user")
                // Linking does not fire AuthStateListener; re-set the user so
                // observeAuthState re-emits with isAnonymous = false immediately.
                auth.updateCurrentUser(firebaseUser).await()
                firebaseUser.toDomainUser()
            }

        override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

        private fun lineProvider(): OAuthProvider =
            OAuthProvider.newBuilder(LINE_PROVIDER_ID)
                .setScopes(listOf("openid", "profile"))
                // LINE's app-to-app auto login returns the callback in a NEW browser
                // tab, losing the Firebase handler's sessionStorage ("missing initial
                // state"). Forcing the web login form keeps the whole round-trip in
                // one Custom Tab, at the cost of typing LINE credentials once.
                .addCustomParameter("disable_auto_login", "true")
                .build()

        private companion object {
            const val LINE_PROVIDER_ID = "oidc.line"
        }

        /** Creates users/{uid} on first sign-in; later sign-ins leave the profile untouched. */
        private suspend fun ensureUserDoc(
            user: FirebaseUser,
            displayNameOverride: String? = null,
        ) {
            val ref = firestore.collection("users").document(user.uid)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "displayName" to (displayNameOverride ?: user.displayName ?: ""),
                        "photoUrl" to user.photoUrl?.toString(),
                        "promptpayId" to null,
                        "fcmTokens" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
        }
    }

private fun FirebaseUser.toDomainUser() =
    User(
        id = uid,
        displayName = displayName ?: "",
        photoUrl = photoUrl?.toString(),
        isAnonymous = isAnonymous,
    )
