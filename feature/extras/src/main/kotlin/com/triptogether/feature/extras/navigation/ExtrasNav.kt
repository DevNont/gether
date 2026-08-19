// Nav files are named <Feature>Nav.kt per docs/05.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.extras.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.extras.ChecklistScreen
import com.triptogether.feature.extras.MeetupEditorScreen
import com.triptogether.feature.extras.MeetupScreen
import com.triptogether.feature.extras.PollsScreen
import kotlinx.serialization.Serializable

@Serializable
data class ChecklistRoute(val tripId: String)

@Serializable
data class PollsRoute(val tripId: String)

@Serializable
data class MeetupRoute(val tripId: String)

@Serializable
data class MeetupEditorRoute(val tripId: String, val meetupId: String? = null)

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

fun NavGraphBuilder.meetupScreen(
    onBack: () -> Unit,
    onOpenEditor: (tripId: String, meetupId: String?) -> Unit,
) {
    composable<MeetupRoute> {
        MeetupScreen(onBack = onBack, onOpenEditor = onOpenEditor)
    }
}

fun NavGraphBuilder.meetupEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    composable<MeetupEditorRoute> {
        MeetupEditorScreen(onDone = onDone, onBack = onBack)
    }
}
