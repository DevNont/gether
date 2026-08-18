package com.triptogether

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.triptogether.feature.plan.navigation.DayPlanRoute
import com.triptogether.feature.plan.navigation.dayPlanScreen
import com.triptogether.feature.trip.navigation.CreateTripRoute
import com.triptogether.feature.trip.navigation.JoinTripRoute
import com.triptogether.feature.trip.navigation.TripListRoute
import com.triptogether.feature.trip.navigation.TripOverviewRoute
import com.triptogether.feature.trip.navigation.createTripScreen
import com.triptogether.feature.trip.navigation.joinTripScreen
import com.triptogether.feature.trip.navigation.tripListScreen
import com.triptogether.feature.trip.navigation.tripOverviewScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    pendingInviteCode: String? = null,
    onInviteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingInviteCode) {
        if (pendingInviteCode != null) {
            navController.navigate(JoinTripRoute(code = pendingInviteCode))
            onInviteConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = TripListRoute,
        modifier = modifier,
    ) {
        tripListScreen(
            onCreateTrip = { navController.navigate(CreateTripRoute) },
            onJoinTrip = { navController.navigate(JoinTripRoute()) },
            onTripClick = { tripId -> navController.navigate(TripOverviewRoute(tripId)) },
        )
        createTripScreen(
            onCreated = { tripId ->
                navController.navigate(TripOverviewRoute(tripId)) {
                    popUpTo(TripListRoute)
                }
            },
            onBack = { navController.popBackStack() },
        )
        joinTripScreen(
            onJoined = { tripId ->
                navController.navigate(TripOverviewRoute(tripId)) {
                    popUpTo(TripListRoute)
                }
            },
            onBack = { navController.popBackStack() },
        )
        tripOverviewScreen(
            onOpenPlan = { tripId -> navController.navigate(DayPlanRoute(tripId)) },
            onBack = { navController.popBackStack() },
        )
        dayPlanScreen(
            // S07 ActivityEditor hooks in with M3.3.
            onAddActivity = {},
            onEditActivity = { _, _ -> },
            onBack = { navController.popBackStack() },
        )
    }
}
