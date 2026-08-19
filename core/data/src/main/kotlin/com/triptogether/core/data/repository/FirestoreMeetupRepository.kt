package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.triptogether.core.data.dto.MeetupDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.Meetup
import com.triptogether.core.domain.repository.MeetupRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreMeetupRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : MeetupRepository {
        override fun observeMeetups(tripId: String): Flow<List<Meetup>> =
            callbackFlow {
                val registration =
                    // Single-field order only (no composite index needed); time is a secondary
                    // client-side sort below.
                    meetupsRef(tripId)
                        .orderBy(FIELD_DATE, Query.Direction.ASCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            trySend(
                                snapshot?.documents
                                    ?.mapNotNull { doc -> doc.toObject(MeetupDto::class.java)?.toDomain(doc.id) }
                                    ?.sortedWith(compareBy({ it.date }, { it.time }))
                                    ?: emptyList(),
                            )
                        }
                awaitClose { registration.remove() }
            }

        override suspend fun upsert(
            tripId: String,
            meetup: Meetup,
        ): Result<Unit> =
            runCatching {
                val meetups = meetupsRef(tripId)
                val ref = if (meetup.id.isBlank()) meetups.document() else meetups.document(meetup.id)
                ref.set(meetup.toDto()).await()
                Unit
            }

        override suspend fun delete(
            tripId: String,
            meetupId: String,
        ): Result<Unit> =
            runCatching {
                meetupsRef(tripId).document(meetupId).delete().await()
                Unit
            }

        private fun meetupsRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(MEETUPS)

        private companion object {
            const val TRIPS = "trips"
            const val MEETUPS = "meetups"
            const val FIELD_DATE = "date"
        }
    }
