package com.triptogether.core.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.triptogether.core.data.awaitWrite
import com.triptogether.core.data.dto.ActivityDto
import com.triptogether.core.data.dto.toDomain
import com.triptogether.core.data.dto.toDto
import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.DayPlan
import com.triptogether.core.domain.repository.NetworkMonitor
import com.triptogether.core.domain.repository.PlanRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestorePlanRepository
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
        private val storage: FirebaseStorage,
        private val networkMonitor: NetworkMonitor,
    ) : PlanRepository {
        /** Day metadata only (date + note); activities stream per day via [observeDayPlan]. */
        override fun observeDayPlans(tripId: String): Flow<List<DayPlan>> =
            callbackFlow {
                val registration =
                    daysRef(tripId)
                        .orderBy(FieldPath.documentId())
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(snapshot?.documents?.mapNotNull { it.toDayPlanShell() } ?: emptyList())
                        }
                awaitClose { registration.remove() }
            }

        override fun observeDayPlan(
            tripId: String,
            dayId: String,
        ): Flow<DayPlan?> =
            combine(dayDocFlow(tripId, dayId), activitiesFlow(tripId, dayId)) { day, activities ->
                day?.copy(activities = activities)
            }

        override suspend fun updateDayNote(
            tripId: String,
            dayId: String,
            note: String?,
        ): Result<Unit> =
            runCatching {
                daysRef(tripId).document(dayId).update("note", note).awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun upsertActivity(
            tripId: String,
            dayId: String,
            activity: Activity,
        ): Result<Unit> =
            runCatching {
                val activities = daysRef(tripId).document(dayId).collection(ACTIVITIES)
                val ref = if (activity.id.isBlank()) activities.document() else activities.document(activity.id)
                ref.set(activity.toDto()).awaitWrite(networkMonitor)
                Unit
            }

        override suspend fun deleteActivity(
            tripId: String,
            dayId: String,
            activityId: String,
        ): Result<Unit> =
            runCatching {
                daysRef(tripId).document(dayId).collection(ACTIVITIES).document(activityId).delete()
                    .awaitWrite(networkMonitor)
                Unit
            }

        /** Storage path per docs/02: trips/{tripId}/attachments/{activityId}/{file}. */
        override suspend fun uploadAttachment(
            tripId: String,
            activityId: String,
            fileName: String,
            bytes: ByteArray,
        ): Result<String> =
            runCatching {
                val ref = storage.reference.child("trips/$tripId/attachments/$activityId/$fileName")
                ref.putBytes(bytes).await()
                ref.downloadUrl.await().toString()
            }

        private fun daysRef(tripId: String): CollectionReference =
            firestore.collection(TRIPS).document(tripId).collection(DAYS)

        private fun dayDocFlow(
            tripId: String,
            dayId: String,
        ): Flow<DayPlan?> =
            callbackFlow {
                val registration =
                    daysRef(tripId).document(dayId).addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toDayPlanShell())
                    }
                awaitClose { registration.remove() }
            }

        private fun activitiesFlow(
            tripId: String,
            dayId: String,
        ): Flow<List<Activity>> =
            callbackFlow {
                val registration =
                    daysRef(tripId).document(dayId).collection(ACTIVITIES)
                        .orderBy(FIELD_SORT_ORDER)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(
                                snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(ActivityDto::class.java)?.toDomain(doc.id)
                                } ?: emptyList(),
                            )
                        }
                awaitClose { registration.remove() }
            }

        private companion object {
            const val TRIPS = "trips"
            const val DAYS = "days"
            const val ACTIVITIES = "activities"
            const val FIELD_SORT_ORDER = "sortOrder"
        }
    }

private fun DocumentSnapshot.toDayPlanShell(): DayPlan? {
    if (!exists()) return null
    // Defensive: a corrupt date string must skip the doc, not crash the listener.
    val date = runCatching { LocalDate.parse(getString("date") ?: id) }.getOrNull() ?: return null
    return DayPlan(
        id = id,
        date = date,
        note = getString("note"),
    )
}
