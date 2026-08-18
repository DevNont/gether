// Nav files are named <Feature>Nav.kt per docs/05.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.extras.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.extras.ChecklistScreen
import com.triptogether.feature.extras.PollsScreen
import kotlinx.serialization.Serializable

@Serializable
data class ChecklistRoute(val tripId: String)

@Serializable
data class PollsRoute(val tripId: String)

fun NavGraphBuilder.checklistScreen(onBack: () -> Unit) {
    composable<ChecklistRoute> {
        ChecklistScreen(onBack = onBack)
    }
}

fun NavGraphBuilder.pollsScreen(onBack: () -> Unit) {
    composable<PollsRoute> {
        PollsScreen(onBack = onBack)
    }
}
