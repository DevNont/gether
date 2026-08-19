package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.triptogether.core.data.awaitWrite
import com.triptogether.core.data.dto.ExpenseDto
import com.triptogether.core.data.dto.ShareDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.domain.repository.ExpenseRepository
import com.triptogether.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreExpenseRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val storage: FirebaseStorage,
        private val networkMonitor: NetworkMonitor,
    ) : ExpenseRepository {
        override fun observeExpenses(tripId: String): Flow<List<Expense>> =
            callbackFlow {
                val registration =
                    expensesRef(tripId)
                        .orderBy(FIELD_DATE, Query.Direction.DESCENDING)
                        .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(
                                snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(ExpenseDto::class.java)?.toDomain(doc.id)
                                } ?: emptyList(),
                            )
                        }
                awaitClose { registration.remove() }
            }

        override suspend fun upsert(
            tripId: String,
            expense: Expense,
        ): Result<Unit> =
            runCatching {
                // Rule 2: the shares must sum to the bill total — reject before any write.
                check(expense.isBalanced) {
                    "Shares sum ${expense.shares.sumOf { it.amount.satang }} != total ${expense.totalAmount.satang}"
                }
                val expenses = expensesRef(tripId)
                val isNew = expense.id.isBlank()
                val ref = if (isNew) expenses.document() else expenses.document(expense.id)
                val timestamps =
                    if (isNew) {
                        mapOf(
                            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                        )
                    } else {
                        mapOf(FIELD_UPDATED_AT to FieldValue.serverTimestamp())
                    }
                ref.set(expense.toDto(), SetOptions.merge()).awaitWrite(networkMonitor)
                ref.set(timestamps, SetOptions.merge()).awaitWrite(networkMonitor)
                Unit
            }

        /**
         * ITEMIZED self-edit: replace only [memberId]'s share and recompute the total inside a
         * transaction, so two members editing their own amounts at once cannot clobber each other.
         */
        override suspend fun updateOwnShare(
            tripId: String,
            expenseId: String,
            memberId: String,
            amount: Money,
        ): Result<Unit> =
            runCatching {
                // Transactions require the server; fail fast instead of hanging offline.
                check(networkMonitor.isOnline.first()) { "offline" }
                require(!amount.isNegative) { "Share amount must not be negative" }
                val ref = expensesRef(tripId).document(expenseId)
                firestore.runTransaction { transaction ->
                    val dto =
                        transaction.get(ref).toObject(ExpenseDto::class.java)
                            ?: error("Expense $expenseId not found")
                    // Self-edit is an ITEMIZED-only affordance for members already on
                    // the bill — other modes and outsiders must go through the editor.
                    check(dto.splitMode == SplitMode.ITEMIZED.name) { "Not an ITEMIZED bill" }
                    val mine =
                        dto.shares.firstOrNull { it.memberId == memberId }
                            ?: error("Member $memberId is not on this bill")
                    val others = dto.shares.filterNot { it.memberId == memberId }
                    val newShares =
                        (others + ShareDto(memberId = memberId, amount = amount.satang, weight = mine.weight))
                            .sortedBy { it.memberId }
                    val newTotal = newShares.sumOf { it.amount }
                    val sharesMap =
                        newShares.map {
                            mapOf(
                                "memberId" to it.memberId,
                                "amount" to it.amount,
                                "weight" to it.weight,
                            )
                        }
                    transaction.set(
                        ref,
                        mapOf(
                            FIELD_SHARES to sharesMap,
                            FIELD_TOTAL_AMOUNT to newTotal,
                            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    null
                }.await()
                Unit
            }

        override suspend fun delete(
            tripId: String,
            expenseId: String,
        ): Result<Unit> =
            runCatching {
                expensesRef(tripId).document(expenseId).delete().awaitWrite(networkMonitor)
                Unit
            }

        /** Storage path per docs/02: trips/{tripId}/slips/{expenseId}/{uuid}.jpg. */
        override suspend fun uploadSlip(
            tripId: String,
            expenseId: String,
            bytes: ByteArray,
        ): Result<String> =
            runCatching {
                val ref = storage.reference.child("trips/$tripId/slips/$expenseId/${UUID.randomUUID()}.jpg")
                ref.putBytes(bytes).await()
                val url = ref.downloadUrl.await().toString()
                expensesRef(tripId).document(expenseId)
                    .update("slipUrls", FieldValue.arrayUnion(url)).await()
                url
            }

        private fun expensesRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(EXPENSES)

        private companion object {
            const val TRIPS = "trips"
            const val EXPENSES = "expenses"
            const val FIELD_DATE = "date"
            const val FIELD_CREATED_AT = "createdAt"
            const val FIELD_UPDATED_AT = "updatedAt"
            const val FIELD_SHARES = "shares"
            const val FIELD_TOTAL_AMOUNT = "totalAmount"
        }
    }
