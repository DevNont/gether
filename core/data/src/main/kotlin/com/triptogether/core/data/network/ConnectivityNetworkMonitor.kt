package com.triptogether.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.triptogether.core.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityNetworkMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NetworkMonitor {
        override val isOnline: Flow<Boolean> =
            callbackFlow {
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // VALIDATED filters out captive portals and dead Wi-Fi that report INTERNET.
                fun NetworkCapabilities.isConnected(): Boolean =
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                fun current(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)?.isConnected() == true

                // Track validated networks ourselves: during onLost, activeNetwork can
                // still return the dying network with stale capabilities, which would
                // leave the flow stuck at "online" until some other callback fires.
                val validated = mutableSetOf<Network>()
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            if (networkCapabilities.isConnected()) validated.add(network) else validated.remove(network)
                            trySend(validated.isNotEmpty())
                        }

                        override fun onLost(network: Network) {
                            validated.remove(network)
                            trySend(validated.isNotEmpty())
                        }
                    }
                trySend(current())
                manager.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    callback,
                )
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }.distinctUntilChanged()
    }
