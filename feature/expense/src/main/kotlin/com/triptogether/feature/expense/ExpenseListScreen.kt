package com.triptogether.feature.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Expense
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** S08 — expenses grouped by day with category filter and trip/my totals header. */
@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseListContent(
        uiState = uiState,
        onFilterChange = viewModel::setFilter,
        onAddExpense = onAddExpense,
        onExpenseClick = onExpenseClick,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseListContent(
    uiState: ExpenseListUiState,
    onFilterChange: (ExpenseCategory?) -> Unit,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.expense_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.expense_list_add),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TotalsHeader(tripTotal = uiState.tripTotal, myTotal = uiState.myTotal)
            CategoryFilterBar(selected = uiState.filter, onFilterChange = onFilterChange)
            when {
                uiState.isLoading ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                uiState.isEmpty ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.expense_list_empty))
                    }
                else -> GroupedList(uiState = uiState, onExpenseClick = onExpenseClick)
            }
        }
    }
}

@Composable
private fun TotalsHeader(
    tripTotal: Money,
    myTotal: Money,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.expense_list_trip_total),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = tripTotal.formatWithSymbol(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.expense_list_my_total),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = myTotal.formatWithSymbol(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterBar(
    selected: ExpenseCategory?,
    onFilterChange: (ExpenseCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onFilterChange(null) },
                label = { Text(stringResource(R.string.expense_filter_all)) },
            )
        }
        items(ExpenseCategory.entries) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onFilterChange(category) },
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@Composable
private fun GroupedList(
    uiState: ExpenseListUiState,
    onExpenseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
    ) {
        if (uiState.prepaid.isNotEmpty()) {
            item(key = "header-prepaid") {
                SectionHeader(
                    label = stringResource(R.string.expense_prepaid_group),
                    total = uiState.prepaidTotal,
                )
            }
            items(uiState.prepaid, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    paidByName = uiState.memberName(expense.paidByMemberId),
                    myShare = uiState.myMemberId?.let { expense.shareOf(it) } ?: Money.ZERO,
                    onClick = { onExpenseClick(expense.id) },
                )
            }
        }
        uiState.groups.forEach { group ->
            item(key = "header-${group.date}") {
                DayHeader(date = group.date, total = group.total)
            }
            items(group.expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    paidByName = uiState.memberName(expense.paidByMemberId),
                    myShare = uiState.myMemberId?.let { expense.shareOf(it) } ?: Money.ZERO,
                    onClick = { onExpenseClick(expense.id) },
                )
            }
        }
    }
}

/** Group header for a non-date section (e.g. prepaid), mirroring DayHeader. */
@Composable
private fun SectionHeader(
    label: String,
    total: Money,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = total.formatWithSymbol(),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    total: Money,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("E d MMM yy")),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = total.formatWithSymbol(),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    paidByName: String,
    myShare: Money,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = expense.category.icon(),
                contentDescription = stringResource(expense.category.labelRes()),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.expense_paid_by, paidByName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = expense.totalAmount.formatWithSymbol(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!myShare.isZero) {
                    Text(
                        text = stringResource(R.string.expense_my_share, myShare.formatWithSymbol()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun ExpenseCategory.labelRes(): Int =
    when (this) {
        ExpenseCategory.FOOD -> R.string.expense_category_food
        ExpenseCategory.STAY -> R.string.expense_category_stay
        ExpenseCategory.TRANSPORT -> R.string.expense_category_transport
        ExpenseCategory.ACTIVITY -> R.string.expense_category_activity
        ExpenseCategory.OTHER -> R.string.expense_category_other
    }

internal fun ExpenseCategory.icon(): ImageVector =
    when (this) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.STAY -> Icons.Default.Hotel
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.ACTIVITY -> Icons.Default.Attractions
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
    }

@Preview(showBackground = true)
@Composable
private fun ExpenseListContentPreview() {
    val expense =
        Expense(
            id = "e1",
            title = "มื้อเย็น — ร้านหมูกระทะ",
            totalAmount = Money(185_000),
            paidByMemberId = "m1",
            date = LocalDate(2026, 12, 5),
            splitMode = SplitMode.EQUAL,
            shares = listOf(Share("m1", Money(92_500)), Share("m2", Money(92_500))),
            createdBy = "m1",
        )
    TripTogetherTheme {
        ExpenseListContent(
            uiState =
                ExpenseListUiState(
                    isLoading = false,
                    groups = listOf(ExpenseDayGroup(expense.date, expense.totalAmount, listOf(expense))),
                    members = listOf(Member(id = "m1", userId = "u1", displayName = "สมชาย")),
                    myMemberId = "m1",
                    tripTotal = Money(185_000),
                    myTotal = Money(92_500),
                ),
            onFilterChange = {},
            onAddExpense = {},
            onExpenseClick = {},
            onBack = {},
        )
    }
}
