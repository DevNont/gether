package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.triptogether.core.data.dto.SettlementDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.Settlement
import com.triptogether.core.domain.model.SettlementStatus
import com.triptogether.core.domain.repository.SettlementRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSettlementRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val storage: FirebaseStorage,
    ) : SettlementRepository {
        override fun observeSettlements(tripId: String): Flow<List<Settlement>> =
            callbackFlow {
                val registration =
                    settlementsRef(tripId).addSnapshotListener { snapshot, _ ->
                        trySend(
                            snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(SettlementDto::class.java)?.toDomain(doc.id)
                            } ?: emptyList(),
                        )
                    }
                awaitClose { registration.remove() }
            }

        override suspend fun create(
            tripId: String,
            settlement: Settlement,
        ): Result<String> =
            runCatching {
                val ref = settlementsRef(tripId).document()
                ref.set(
                    settlement.toDto().copy(status = SettlementStatus.PENDING.name),
                ).await()
                ref.update("createdAt", FieldValue.serverTimestamp()).await()
                ref.id
            }

        override suspend fun confirm(
            tripId: String,
            settlementId: String,
            confirmedByMemberId: String,
        ): Result<Unit> =
            runCatching {
                settlementsRef(tripId).document(settlementId).update(
                    mapOf(
                        "status" to SettlementStatus.CONFIRMED.name,
                        "confirmedBy" to confirmedByMemberId,
                        "confirmedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                Unit
            }

        override suspend fun delete(
            tripId: String,
            settlementId: String,
        ): Result<Unit> =
            runCatching {
                settlementsRef(tripId).document(settlementId).delete().await()
                Unit
            }

        override suspend fun uploadSlip(
            tripId: String,
            settlementId: String,
            bytes: ByteArray,
        ): Result<String> =
            runCatching {
                val ref =
                    storage.reference.child(
                        "trips/$tripId/slips/settlements/$settlementId/${UUID.randomUUID()}.jpg",
                    )
                ref.putBytes(bytes).await()
                val url = ref.downloadUrl.await().toString()
                settlementsRef(tripId).document(settlementId).update("slipUrl", url).await()
                url
            }

        private fun settlementsRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(SETTLEMENTS)

        private companion object {
            const val TRIPS = "trips"
            const val SETTLEMENTS = "settlements"
        }
    }
