package com.triptogether.core.data.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
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

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<User> =
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val firebaseUser =
                    auth.signInWithCredential(credential).await().user
                        ?: error("Sign-in returned no user")
                ensureUserDoc(firebaseUser)
                firebaseUser.toDomainUser()
            }

        /**
         * LINE Login through Firebase's OIDC provider "oidc.line" (configured in the
         * Auth console with LINE's issuer https://access.line.me). Browser flow, so it
         * works without Play Services and without a custom-token backend.
         */
        override suspend fun signInWithLine(host: AuthUiHost): Result<User> =
            runCatching {
                val activity = host as? Activity ?: error("AuthUiHost must be an Activity")
                val provider =
                    OAuthProvider.newBuilder(LINE_PROVIDER_ID)
                        .setScopes(listOf("openid", "profile"))
                        .build()
                val result =
                    auth.pendingAuthResult?.await()
                        ?: auth.startActivityForSignInWithProvider(activity, provider).await()
                val firebaseUser = result.user ?: error("Sign-in returned no user")
                ensureUserDoc(firebaseUser)
                firebaseUser.toDomainUser()
            }

        override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

        private companion object {
            const val LINE_PROVIDER_ID = "oidc.line"
        }

        /** Creates users/{uid} on first sign-in; later sign-ins leave the profile untouched. */
        private suspend fun ensureUserDoc(user: FirebaseUser) {
            val ref = firestore.collection("users").document(user.uid)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "displayName" to (user.displayName ?: ""),
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
    )
