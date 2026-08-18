package com.triptogether.core.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : UserRepository {
        override fun observeUser(userId: String): Flow<User?> =
            callbackFlow {
                val registration =
                    firestore.collection(USERS).document(userId).addSnapshotListener { snapshot, _ ->
                        trySend(snapshot?.takeIf { it.exists() }?.toDomainUser(userId))
                    }
                awaitClose { registration.remove() }
            }

        override suspend fun updateProfile(user: User): Result<Unit> =
            runCatching {
                firestore.collection(USERS).document(user.id).set(
                    mapOf(
                        "displayName" to user.displayName,
                        "photoUrl" to user.photoUrl,
                        "promptpayId" to user.promptpayId,
                    ),
                    SetOptions.merge(),
                ).await()
                Unit
            }

        override suspend fun registerPushToken(
            userId: String,
            token: String,
        ): Result<Unit> =
            runCatching {
                firestore.collection(USERS).document(userId)
                    .update("fcmTokens", FieldValue.arrayUnion(token)).await()
                Unit
            }

        private companion object {
            const val USERS = "users"
        }
    }

private fun DocumentSnapshot.toDomainUser(id: String) =
    User(
        id = id,
        displayName = getString("displayName") ?: "",
        photoUrl = getString("photoUrl"),
        promptpayId = getString("promptpayId"),
    )
