package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.triptogether.core.data.dto.ChecklistItemDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.ChecklistItem
import com.triptogether.core.domain.repository.ChecklistRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreChecklistRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : ChecklistRepository {
        override fun observeItems(tripId: String): Flow<List<ChecklistItem>> =
            callbackFlow {
                val registration =
                    itemsRef(tripId).orderBy(FIELD_SORT_ORDER).addSnapshotListener { snapshot, _ ->
                        trySend(
                            snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(ChecklistItemDto::class.java)?.toDomain(doc.id)
                            } ?: emptyList(),
                        )
                    }
                awaitClose { registration.remove() }
            }

        override suspend fun upsert(
            tripId: String,
            item: ChecklistItem,
        ): Result<Unit> =
            runCatching {
                val items = itemsRef(tripId)
                val ref = if (item.id.isBlank()) items.document() else items.document(item.id)
                ref.set(item.toDto()).await()
                Unit
            }

        override suspend fun delete(
            tripId: String,
            itemId: String,
        ): Result<Unit> =
            runCatching {
                itemsRef(tripId).document(itemId).delete().await()
                Unit
            }

        override suspend fun setChecked(
            tripId: String,
            itemId: String,
            memberId: String,
            checked: Boolean,
        ): Result<Unit> =
            runCatching {
                val op = if (checked) FieldValue.arrayUnion(memberId) else FieldValue.arrayRemove(memberId)
                itemsRef(tripId).document(itemId).update("checkedBy", op).await()
                Unit
            }

        private fun itemsRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(CHECKLIST)

        private companion object {
            const val TRIPS = "trips"
            const val CHECKLIST = "checklist"
            const val FIELD_SORT_ORDER = "sortOrder"
        }
    }
