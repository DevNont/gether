// Nav files are named <Feature>Nav.kt per docs/05.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.auth.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsRoute> {
        SettingsScreen(onBack = onBack)
    }
}
