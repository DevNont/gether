// Nav files are named <Feature>Nav.kt per docs/05.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.auth.SettingsLanguageScreen
import com.triptogether.feature.auth.SettingsProfileScreen
import com.triptogether.feature.auth.SettingsPromptpayScreen
import com.triptogether.feature.auth.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

@Serializable
data object SettingsProfileRoute

@Serializable
data object SettingsPromptpayRoute

@Serializable
data object SettingsLanguageRoute

fun NavGraphBuilder.settingsScreen(
    onOpenProfile: () -> Unit,
    onOpenPromptpay: () -> Unit,
    onOpenLanguage: () -> Unit,
    onBack: () -> Unit,
) {
    composable<SettingsRoute> {
        SettingsScreen(
            onOpenProfile = onOpenProfile,
            onOpenPromptpay = onOpenPromptpay,
            onOpenLanguage = onOpenLanguage,
            onBack = onBack,
        )
    }
    composable<SettingsProfileRoute> {
        SettingsProfileScreen(onBack = onBack)
    }
    composable<SettingsPromptpayRoute> {
        SettingsPromptpayScreen(onBack = onBack)
    }
    composable<SettingsLanguageRoute> {
        SettingsLanguageScreen(onBack = onBack)
    }
}
