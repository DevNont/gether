package com.triptogether.feature.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter

/** S03 — name + date range (max 60 days); cover image arrives once Storage is available. */
@Composable
fun CreateTripScreen(
    onCreated: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTripViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateTripEvent.Created -> onCreated(event.tripId)
                is CreateTripEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    CreateTripContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::onNameChange,
        onDatesSelected = viewModel::onDatesSelected,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )

    if (uiState.showShrinkConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissShrinkConfirm,
            title = { Text(stringResource(R.string.edit_trip_shrink_title)) },
            text = { Text(stringResource(R.string.edit_trip_shrink_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::save) {
                    Text(
                        text = stringResource(R.string.edit_trip_shrink_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissShrinkConfirm) {
                    Text(stringResource(R.string.create_trip_dates_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripContent(
    uiState: CreateTripUiState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onDatesSelected: (LocalDate, LocalDate) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleRes =
                        if (uiState.isExisting) R.string.edit_trip_title else R.string.create_trip_title
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.create_trip_back),
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.create_trip_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                Text(
                    text = dateRangeLabel(uiState.startDate, uiState.endDate),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (uiState.isRangeTooLong) {
                Text(
                    text = stringResource(R.string.create_trip_range_too_long, MAX_TRIP_DAYS),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            uiState.dateErrorResId?.let { resId ->
                Text(
                    text = stringResource(resId, uiState.dateErrorArg),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    val saveRes =
                        if (uiState.isExisting) R.string.edit_trip_save else R.string.create_trip_save
                    Text(stringResource(saveRes))
                }
            }
        }
    }

    if (showDatePicker) {
        TripDateRangePickerDialog(
            // Past dates are only pickable when editing an already-started trip.
            allowPastDates = uiState.isExisting,
            onConfirm = { start, end ->
                onDatesSelected(start, end)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangePickerDialog(
    allowPastDates: Boolean,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDateRangePickerState(
            selectableDates =
                if (allowPastDates) {
                    DatePickerDefaults.AllDates
                } else {
                    remember { futureOnlyDates() }
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null,
                onClick = {
                    val start = pickerState.selectedStartDateMillis ?: return@TextButton
                    val end = pickerState.selectedEndDateMillis ?: return@TextButton
                    onConfirm(start.toUtcLocalDate(), end.toUtcLocalDate())
                },
            ) {
                Text(stringResource(R.string.create_trip_dates_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.create_trip_dates_cancel))
            }
        },
    ) {
        DateRangePicker(state = pickerState, showModeToggle = false)
    }
}

/** The Material picker reports UTC-midnight millis, so convert back in UTC. */
private fun Long.toUtcLocalDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

/** Greys out days before today in the range picker (new trips only). */
@OptIn(ExperimentalMaterial3Api::class)
private fun futureOnlyDates(): SelectableDates {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toUtcLocalDate() >= today

        override fun isSelectableYear(year: Int): Boolean = year >= today.year
    }
}

@Composable
private fun dateRangeLabel(
    start: LocalDate?,
    end: LocalDate?,
): String =
    if (start != null && end != null) {
        val formatter = DateTimeFormatter.ofPattern("d MMM yy")
        "${formatter.format(start.toJavaLocalDate())} – ${formatter.format(end.toJavaLocalDate())}"
    } else {
        stringResource(R.string.create_trip_dates_label)
    }

@Preview(showBackground = true)
@Composable
private fun CreateTripContentPreview() {
    TripTogetherTheme {
        CreateTripContent(
            uiState =
                CreateTripUiState(
                    name = "ทริปเชียงใหม่",
                    startDate = LocalDate(2026, 12, 5),
                    endDate = LocalDate(2026, 12, 7),
                ),
            snackbarHostState = SnackbarHostState(),
            onNameChange = {},
            onDatesSelected = { _, _ -> },
            onSave = {},
            onBack = {},
        )
    }
}
