package com.triptogether.feature.trip.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.trip.CreateTripScreen
import com.triptogether.feature.trip.TripListScreen
import kotlinx.serialization.Serializable

@Serializable
data object TripListRoute

@Serializable
data object CreateTripRoute

fun NavGraphBuilder.tripListScreen(
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onTripClick: (String) -> Unit,
) {
    composable<TripListRoute> {
        TripListScreen(
            onCreateTrip = onCreateTrip,
            onJoinTrip = onJoinTrip,
            onTripClick = onTripClick,
        )
    }
}

fun NavGraphBuilder.createTripScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable<CreateTripRoute> {
        CreateTripScreen(onCreated = onCreated, onBack = onBack)
    }
}
