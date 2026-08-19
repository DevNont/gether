package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.triptogether.core.data.awaitWrite
import com.triptogether.core.data.dto.PollDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.Poll
import com.triptogether.core.domain.repository.NetworkMonitor
import com.triptogether.core.domain.repository.PollRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestorePollRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val networkMonitor: NetworkMonitor,
    ) : PollRepository {
        override fun observePolls(tripId: String): Flow<List<Poll>> =
            callbackFlow {
                val registration =
                    pollsRef(tripId).addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(
                            snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(PollDto::class.java)?.toDomain(doc.id)
                            } ?: emptyList(),
                        )
                    }
                awaitClose { registration.remove() }
            }

        override suspend fun create(
            tripId: String,
            poll: Poll,
        ): Result<String> =
            runCatching {
                val ref = pollsRef(tripId).document()
                ref.set(poll.toDto()).awaitWrite(networkMonitor)
                ref.id
            }

        /**
         * Votes live inside the options array, so this is a transaction:
         * read, rewrite the voter sets, write back atomically.
         */
        override suspend fun setVote(
            tripId: String,
            pollId: String,
            optionId: String,
            memberId: String,
            selected: Boolean,
        ): Result<Unit> =
            runCatching {
                val ref = pollsRef(tripId).document(pollId)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(ref)
                    val poll = snapshot.toObject(PollDto::class.java) ?: return@runTransaction
                    check(!poll.closed) { "Poll is closed" }
                    val updated =
                        poll.options.map { option ->
                            val voters = option.voterMemberIds.toMutableSet()
                            when {
                                option.id == optionId && selected -> voters.add(memberId)
                                option.id == optionId && !selected -> voters.remove(memberId)
                                // Single-choice: picking one clears this member from the others.
                                !poll.multiChoice && selected -> voters.remove(memberId)
                            }
                            option.copy(voterMemberIds = voters.toList())
                        }
                    transaction.update(ref, "options", updated.map { optionToMap(it) })
                }.await()
                Unit
            }

        override suspend fun close(
            tripId: String,
            pollId: String,
        ): Result<Unit> =
            runCatching {
                pollsRef(tripId).document(pollId).update("closed", true).awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun delete(
            tripId: String,
            pollId: String,
        ): Result<Unit> =
            runCatching {
                pollsRef(tripId).document(pollId).delete().awaitWrite(networkMonitor)
                Unit
            }

        private fun optionToMap(option: com.triptogether.core.data.dto.PollOptionDto): Map<String, Any> =
            mapOf(
                "id" to option.id,
                "label" to option.label,
                "voterMemberIds" to option.voterMemberIds,
            )

        private fun pollsRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(POLLS)

        private companion object {
            const val TRIPS = "trips"
            const val POLLS = "polls"
        }
    }
