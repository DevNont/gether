// Nav files are named <Feature>Nav.kt per docs/05, even with a single route class so far.
@file:Suppress("MatchingDeclarationName")

package com.triptogether.feature.expense.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.triptogether.feature.expense.ExpenseDetailScreen
import com.triptogether.feature.expense.ExpenseEditorScreen
import com.triptogether.feature.expense.ExpenseListScreen
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseListRoute(val tripId: String)

@Serializable
data class ExpenseEditorRoute(
    val tripId: String,
    val expenseId: String? = null,
)

@Serializable
data class ExpenseDetailRoute(
    val tripId: String,
    val expenseId: String,
)

fun NavGraphBuilder.expenseDetailScreen(
    onEdit: (ExpenseEditorRoute) -> Unit,
    onBack: () -> Unit,
) {
    composable<ExpenseDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExpenseDetailRoute>()
        ExpenseDetailScreen(
            onEdit = { onEdit(ExpenseEditorRoute(tripId = route.tripId, expenseId = route.expenseId)) },
            onBack = onBack,
        )
    }
}

fun NavGraphBuilder.expenseEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ExpenseEditorRoute> {
        ExpenseEditorScreen(onDone = onDone, onBack = onBack)
    }
}

fun NavGraphBuilder.expenseListScreen(
    onAddExpense: (String) -> Unit,
    onExpenseClick: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    composable<ExpenseListRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExpenseListRoute>()
        ExpenseListScreen(
            onAddExpense = { onAddExpense(route.tripId) },
            onExpenseClick = { expenseId -> onExpenseClick(route.tripId, expenseId) },
            onBack = onBack,
        )
    }
}
