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

/** tripId null = create; non-null = edit that trip (S05 owner menu). */
@Serializable
data class CreateTripRoute(val tripId: String? = null)

@Serializable
data class JoinTripRoute(val code: String? = null)

@Serializable
data class TripOverviewRoute(val tripId: String)

fun NavGraphBuilder.tripListScreen(
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onTripClick: (String) -> Unit,
    onEditTrip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    devMode: Boolean = false,
) {
    composable<TripListRoute> {
        TripListScreen(
            onCreateTrip = onCreateTrip,
            onJoinTrip = onJoinTrip,
            onTripClick = onTripClick,
            onEditTrip = onEditTrip,
            onOpenSettings = onOpenSettings,
            devMode = devMode,
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
    onOpenPolls: (String) -> Unit,
    onEditTrip: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    composable<TripOverviewRoute> {
        TripOverviewScreen(
            onOpenPlan = onOpenPlan,
            onOpenExpenses = onOpenExpenses,
            onOpenSettlement = onOpenSettlement,
            onOpenChecklist = onOpenChecklist,
            onOpenPolls = onOpenPolls,
            onEditTrip = onEditTrip,
            onDeleted = onDeleted,
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
