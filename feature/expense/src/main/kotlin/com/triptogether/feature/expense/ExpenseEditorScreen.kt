package com.triptogether.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.ExpenseCategory
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.SplitMode
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

/** S09 — the most important screen. All split maths comes from domain ExpenseSplitter; never recompute here. */
@Composable
fun ExpenseEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ExpenseEditorEvent.Saved -> onDone()
                is ExpenseEditorEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    ExpenseEditorContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        callbacks =
            ExpenseEditorCallbacks(
                onTitleChange = viewModel::onTitleChange,
                onCategoryChange = viewModel::onCategoryChange,
                onTotalChange = viewModel::onTotalChange,
                onPaidByChange = viewModel::onPaidByChange,
                onDateChange = viewModel::onDateChange,
                onTimeChange = viewModel::onTimeChange,
                onSplitModeChange = viewModel::onSplitModeChange,
                onMemberToggle = viewModel::onMemberToggle,
                onAmountChange = viewModel::onAmountChange,
                onWeightChange = viewModel::onWeightChange,
                onDistributeRemainder = viewModel::distributeRemainder,
                onApplyServiceCharge = viewModel::applyServiceCharge,
                onSave = viewModel::save,
                onBack = onBack,
            ),
        modifier = modifier,
    )
}

/** Bundled callbacks keep the stateless composable's parameter list sane. */
data class ExpenseEditorCallbacks(
    val onTitleChange: (String) -> Unit,
    val onCategoryChange: (ExpenseCategory) -> Unit,
    val onTotalChange: (String) -> Unit,
    val onPaidByChange: (String) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onTimeChange: (LocalTime?) -> Unit,
    val onSplitModeChange: (SplitMode) -> Unit,
    val onMemberToggle: (String) -> Unit,
    val onAmountChange: (String, String) -> Unit,
    val onWeightChange: (String, Int) -> Unit,
    val onDistributeRemainder: () -> Unit,
    val onApplyServiceCharge: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseEditorContent(
    uiState: ExpenseEditorUiState,
    snackbarHostState: SnackbarHostState,
    callbacks: ExpenseEditorCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleRes =
                        if (uiState.isExisting) {
                            R.string.expense_editor_title_edit
                        } else {
                            R.string.expense_editor_title_new
                        }
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = callbacks.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.expense_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(32.dp))
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = callbacks.onTitleChange,
                label = { Text(stringResource(R.string.expense_editor_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("expense_title"),
            )
            CategoryPicker(selected = uiState.category, onSelect = callbacks.onCategoryChange)
            OutlinedTextField(
                value = uiState.totalInput,
                onValueChange = callbacks.onTotalChange,
                label = { Text(stringResource(R.string.expense_editor_total)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("expense_total"),
            )
            PaidByDropdown(
                rows = uiState.rows.map { it.member },
                selectedId = uiState.paidByMemberId,
                onSelect = callbacks.onPaidByChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    date = uiState.date,
                    onDateChange = callbacks.onDateChange,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    time = uiState.time,
                    onTimeChange = callbacks.onTimeChange,
                    modifier = Modifier.weight(1f),
                )
            }
            if (uiState.isPrepaid) {
                Text(
                    text = stringResource(R.string.expense_editor_prepaid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            SplitModeSelector(selected = uiState.splitMode, onSelect = callbacks.onSplitModeChange)
            uiState.rows.forEach { row ->
                MemberRow(
                    row = row,
                    mode = uiState.splitMode,
                    computedAmount = uiState.computedAmountFor(row.member.id),
                    onToggle = { callbacks.onMemberToggle(row.member.id) },
                    onAmountChange = { callbacks.onAmountChange(row.member.id, it) },
                    onWeightChange = { callbacks.onWeightChange(row.member.id, it) },
                )
            }
            if (uiState.splitMode == SplitMode.EXACT) {
                ExactModeFooter(uiState = uiState, callbacks = callbacks)
            }
            // Slip capture (CameraX + Storage) arrives with M4.6 once the Blaze bucket exists.
            Button(
                onClick = callbacks.onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().testTag("expense_save"),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(stringResource(R.string.expense_editor_save))
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ExpenseCategory.entries.size) { index ->
            val category = ExpenseCategory.entries[index]
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaidByDropdown(
    rows: List<Member>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = rows.firstOrNull { it.id == selectedId }?.displayName ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.expense_editor_paid_by)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            rows.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.displayName) },
                    onClick = {
                        onSelect(member.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
        Text(
            text =
                date?.toJavaLocalDate()?.format(DateTimeFormatter.ofPattern("d MMM yy"))
                    ?: stringResource(R.string.expense_editor_date),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date,
                            )
                        }
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.expense_editor_date_ok))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    time: LocalTime?,
    onTimeChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Icon(imageVector = Icons.Default.Schedule, contentDescription = null)
        Text(
            text = time?.let { formatExpenseTime(it) } ?: stringResource(R.string.expense_editor_time),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
    if (showPicker) {
        val pickerState =
            rememberTimePickerState(
                initialHour = time?.hour ?: DEFAULT_PICKER_HOUR,
                initialMinute = time?.minute ?: 0,
                is24Hour = true,
            )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.expense_editor_time)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime(pickerState.hour, pickerState.minute))
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.expense_editor_date_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onTimeChange(null)
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.expense_editor_time_clear))
                }
            },
        )
    }
}

private const val DEFAULT_PICKER_HOUR = 9

internal fun formatExpenseTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitModeSelector(
    selected: SplitMode,
    onSelect: (SplitMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SplitMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = SplitMode.entries.size),
                modifier = Modifier.testTag("mode_${mode.name}"),
            ) {
                Text(stringResource(mode.labelRes()))
            }
        }
    }
}

@Composable
private fun MemberRow(
    row: MemberSplitRow,
    mode: SplitMode,
    computedAmount: Money?,
    onToggle: () -> Unit,
    onAmountChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = row.selected, onCheckedChange = { onToggle() })
        Text(
            text = row.member.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (!row.selected) return@Row
        when (mode) {
            SplitMode.EQUAL ->
                Text(
                    text = computedAmount?.formatWithSymbol() ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                )
            SplitMode.EXACT ->
                OutlinedTextField(
                    value = row.amountInput,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") },
                    modifier = Modifier.width(120.dp).testTag("amount_${row.member.id}"),
                )
            SplitMode.SHARES ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { onWeightChange(-1) }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.expense_editor_weight_down),
                        )
                    }
                    Text(text = row.weight.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onWeightChange(1) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.expense_editor_weight_up),
                        )
                    }
                    Text(
                        text = computedAmount?.formatWithSymbol() ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp),
                    )
                }
        }
    }
}

/** The running delta bar plus the two helper buttons — docs/04 §2.2, never auto-corrects silently. */
@Composable
private fun ExactModeFooter(
    uiState: ExpenseEditorUiState,
    callbacks: ExpenseEditorCallbacks,
    modifier: Modifier = Modifier,
) {
    val delta = uiState.delta
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val (barColor, barText) =
            when {
                delta == null ->
                    MaterialTheme.colorScheme.surfaceVariant to
                        stringResource(R.string.expense_delta_enter_total)
                delta.isZero ->
                    MaterialTheme.colorScheme.primaryContainer to
                        stringResource(R.string.expense_delta_ok)
                delta.isNegative ->
                    MaterialTheme.colorScheme.errorContainer to
                        stringResource(R.string.expense_delta_missing, delta.abs().formatWithSymbol())
                else ->
                    MaterialTheme.colorScheme.errorContainer to
                        stringResource(R.string.expense_delta_over, delta.formatWithSymbol())
            }
        Text(
            text = barText,
            style = MaterialTheme.typography.titleSmall,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(barColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .testTag("delta_bar"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = callbacks.onDistributeRemainder,
                enabled = !uiState.everyoneEntered && uiState.total != null,
                modifier = Modifier.weight(1f).testTag("distribute_button"),
            ) {
                Text(stringResource(R.string.expense_distribute_remainder))
            }
            OutlinedButton(
                onClick = callbacks.onApplyServiceCharge,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.expense_service_charge))
            }
        }
    }
}

internal fun SplitMode.labelRes(): Int =
    when (this) {
        SplitMode.EQUAL -> R.string.expense_mode_equal
        SplitMode.EXACT -> R.string.expense_mode_exact
        SplitMode.SHARES -> R.string.expense_mode_shares
    }

@Preview(showBackground = true)
@Composable
private fun ExpenseEditorContentPreview() {
    val members =
        listOf(
            Member(id = "m1", userId = "u1", displayName = "สมชาย"),
            Member(id = "m2", userId = null, displayName = "น้องแมว"),
        )
    TripTogetherTheme {
        ExpenseEditorContent(
            uiState =
                ExpenseEditorUiState(
                    isLoading = false,
                    title = "มื้อเย็น",
                    totalInput = "1,850.00",
                    paidByMemberId = "m1",
                    date = LocalDate(2026, 12, 5),
                    splitMode = SplitMode.EXACT,
                    rows = members.map { MemberSplitRow(member = it) },
                ),
            snackbarHostState = SnackbarHostState(),
            callbacks =
                ExpenseEditorCallbacks(
                    onTitleChange = {},
                    onCategoryChange = {},
                    onTotalChange = {},
                    onPaidByChange = {},
                    onDateChange = {},
                    onTimeChange = {},
                    onSplitModeChange = {},
                    onMemberToggle = {},
                    onAmountChange = { _, _ -> },
                    onWeightChange = { _, _ -> },
                    onDistributeRemainder = {},
                    onApplyServiceCharge = {},
                    onSave = {},
                    onBack = {},
                ),
        )
    }
}
