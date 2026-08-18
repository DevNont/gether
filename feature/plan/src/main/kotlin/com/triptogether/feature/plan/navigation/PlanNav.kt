// Nav files are named <Feature>Nav.kt per docs/05, even with a single route class so far.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.plan.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.triptogether.feature.plan.ActivityEditorScreen
import com.triptogether.feature.plan.DayPlanScreen
import kotlinx.serialization.Serializable

@Serializable
data class DayPlanRoute(val tripId: String)

@Serializable
data class ActivityEditorRoute(
    val tripId: String,
    val dayId: String,
    val activityId: String? = null,
)

fun NavGraphBuilder.dayPlanScreen(
    onAddActivity: (ActivityEditorRoute) -> Unit,
    onEditActivity: (ActivityEditorRoute) -> Unit,
    onBack: () -> Unit,
) {
    composable<DayPlanRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DayPlanRoute>()
        DayPlanScreen(
            onAddActivity = { dayId ->
                onAddActivity(ActivityEditorRoute(tripId = route.tripId, dayId = dayId))
            },
            onEditActivity = { dayId, activityId ->
                onEditActivity(
                    ActivityEditorRoute(tripId = route.tripId, dayId = dayId, activityId = activityId),
                )
            },
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.activityEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ActivityEditorRoute> {
        ActivityEditorScreen(onDone = onDone, onBack = onBack)
    }
}
