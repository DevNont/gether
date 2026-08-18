// Nav files are named <Feature>Nav.kt per docs/05, even with a single route class so far.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.plan.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.plan.DayPlanScreen
import kotlinx.serialization.Serializable

@Serializable
data class DayPlanRoute(val tripId: String)

fun NavGraphBuilder.dayPlanScreen(
    onAddActivity: (String) -> Unit,
    onEditActivity: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    composable<DayPlanRoute> {
        DayPlanScreen(
            onAddActivity = onAddActivity,
            onEditActivity = onEditActivity,
            onBack = onBack,
        )
    }
}
