package com.triptogether

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.triptogether.feature.trip.navigation.CreateTripRoute
import com.triptogether.feature.trip.navigation.TripListRoute
import com.triptogether.feature.trip.navigation.createTripScreen
import com.triptogether.feature.trip.navigation.tripListScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TripListRoute,
        modifier = modifier,
    ) {
        tripListScreen(
            onCreateTrip = { navController.navigate(CreateTripRoute) },
            // S04 JoinTrip (M2.5) and S05 Overview (M2.6) hook in here.
            onJoinTrip = {},
            onTripClick = {},
        )
        createTripScreen(
            // Lands on S05 Overview once M2.6 exists; back to the list for now.
            onCreated = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
