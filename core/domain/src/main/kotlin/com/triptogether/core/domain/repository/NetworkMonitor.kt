package com.triptogether.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Connectivity signal for the thin offline banner (docs/03 cross-cutting). */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
