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

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            // The new network may not be validated yet; onCapabilitiesChanged follows.
                            trySend(current())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            trySend(networkCapabilities.isConnected())
                        }

                        override fun onLost(network: Network) {
                            trySend(current())
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
