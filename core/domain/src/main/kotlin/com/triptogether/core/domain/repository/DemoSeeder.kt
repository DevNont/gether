package com.triptogether.core.domain.repository

/** Debug-only: creates one fully-populated demo trip. Returns the new tripId. */
interface DemoSeeder {
    suspend fun seedJapanTrip(): Result<String>
}
