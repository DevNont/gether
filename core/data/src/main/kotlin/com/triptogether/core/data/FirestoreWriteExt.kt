package com.triptogether.core.data

import com.google.android.gms.tasks.Task
import com.triptogether.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Awaits a Firestore write only while online. Offline, the SDK has already
 * committed the write to the local cache and will sync later — awaiting the
 * server ack would suspend forever, so we return immediately instead.
 */
suspend fun Task<*>.awaitWrite(networkMonitor: NetworkMonitor) {
    if (networkMonitor.isOnline.first()) {
        await()
    }
}
