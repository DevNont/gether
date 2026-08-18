// Nav files are named <Feature>Nav.kt per docs/05, even with a single route class so far.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.settlement.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.triptogether.feature.settlement.SettlementScreen
import kotlinx.serialization.Serializable

@Serializable
data class SettlementRoute(val tripId: String)

fun NavGraphBuilder.settlementScreen(onBack: () -> Unit) {
    composable<SettlementRoute> {
        SettlementScreen(onBack = onBack)
    }
}
