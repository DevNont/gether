package com.triptogether.core.domain.repository

/** Thin analytics facade so features never touch Firebase directly. */
interface AnalyticsLogger {
    fun log(
        event: String,
        params: Map<String, String> = emptyMap(),
    )

    companion object Events {
        const val TRIP_CREATED = "trip_created"
        const val TRIP_JOINED = "trip_joined"
        const val EXPENSE_SAVED = "expense_saved"
        const val SETTLEMENT_CONFIRMED = "settlement_confirmed"
    }
}
