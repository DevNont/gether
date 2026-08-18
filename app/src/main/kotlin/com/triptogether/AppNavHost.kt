package com.triptogether

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.triptogether.feature.trip.navigation.CreateTripRoute
import com.triptogether.feature.trip.navigation.JoinTripRoute
import com.triptogether.feature.trip.navigation.TripListRoute
import com.triptogether.feature.trip.navigation.createTripScreen
import com.triptogether.feature.trip.navigation.joinTripScreen
import com.triptogether.feature.trip.navigation.tripListScreen

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
            // S05 Overview (M2.6) hooks in here.
            onTripClick = {},
        )
        createTripScreen(
            // Lands on S05 Overview once M2.6 exists; back to the list for now.
            onCreated = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
        joinTripScreen(
            // Lands on S05 Overview once M2.6 exists; back to the list for now.
            onJoined = { navController.popBackStack(TripListRoute, inclusive = false) },
            onBack = { navController.popBackStack() },
        )
    }
}
