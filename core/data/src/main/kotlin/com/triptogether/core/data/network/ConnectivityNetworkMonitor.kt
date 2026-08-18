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

                fun current(): Boolean {
                    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
                    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                }

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(true)
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
