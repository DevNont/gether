package com.triptogether

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.triptogether.feature.expense.navigation.ExpenseDetailRoute
import com.triptogether.feature.expense.navigation.ExpenseEditorRoute
import com.triptogether.feature.expense.navigation.ExpenseListRoute
import com.triptogether.feature.expense.navigation.expenseDetailScreen
import com.triptogether.feature.expense.navigation.expenseEditorScreen
import com.triptogether.feature.expense.navigation.expenseListScreen
import com.triptogether.feature.extras.navigation.ChecklistRoute
import com.triptogether.feature.extras.navigation.checklistScreen
import com.triptogether.feature.plan.navigation.DayPlanRoute
import com.triptogether.feature.plan.navigation.activityEditorScreen
import com.triptogether.feature.plan.navigation.dayPlanScreen
import com.triptogether.feature.settlement.navigation.SettlementRoute
import com.triptogether.feature.settlement.navigation.settlementScreen
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
            onOpenExpenses = { tripId -> navController.navigate(ExpenseListRoute(tripId)) },
            onOpenSettlement = { tripId -> navController.navigate(SettlementRoute(tripId)) },
            onOpenChecklist = { tripId -> navController.navigate(ChecklistRoute(tripId)) },
            onBack = { navController.popBackStack() },
        )
        settlementScreen(onBack = { navController.popBackStack() })
        checklistScreen(onBack = { navController.popBackStack() })
        expenseListScreen(
            onAddExpense = { tripId -> navController.navigate(ExpenseEditorRoute(tripId)) },
            onExpenseClick = { tripId, expenseId ->
                navController.navigate(ExpenseDetailRoute(tripId, expenseId))
            },
            onBack = { navController.popBackStack() },
        )
        expenseEditorScreen(
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
        expenseDetailScreen(
            onEdit = { route -> navController.navigate(route) },
            onBack = { navController.popBackStack() },
        )
        dayPlanScreen(
            onAddActivity = { route -> navController.navigate(route) },
            onEditActivity = { route -> navController.navigate(route) },
            onBack = { navController.popBackStack() },
        )
        activityEditorScreen(
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
