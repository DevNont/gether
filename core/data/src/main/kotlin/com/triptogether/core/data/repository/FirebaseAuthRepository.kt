package com.triptogether.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.AuthRepository
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

        override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut() }

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
