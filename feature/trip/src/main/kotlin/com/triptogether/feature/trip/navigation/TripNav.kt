package com.triptogether.feature.trip.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.triptogether.feature.trip.CreateTripScreen
import com.triptogether.feature.trip.JoinTripScreen
import com.triptogether.feature.trip.TripListScreen
import com.triptogether.feature.trip.TripOverviewScreen
import kotlinx.serialization.Serializable

@Serializable
data object TripListRoute

@Serializable
data object CreateTripRoute

@Serializable
data class JoinTripRoute(val code: String? = null)

@Serializable
data class TripOverviewRoute(val tripId: String)

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

fun NavGraphBuilder.tripOverviewScreen(
    onOpenPlan: (String) -> Unit,
    onOpenExpenses: (String) -> Unit,
    onOpenSettlement: (String) -> Unit,
    onOpenChecklist: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable<TripOverviewRoute> {
        TripOverviewScreen(
            onOpenPlan = onOpenPlan,
            onOpenExpenses = onOpenExpenses,
            onOpenSettlement = onOpenSettlement,
            onOpenChecklist = onOpenChecklist,
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.joinTripScreen(
    onJoined: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable<JoinTripRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<JoinTripRoute>()
        JoinTripScreen(onJoined = onJoined, onBack = onBack, initialCode = route.code)
    }
}
