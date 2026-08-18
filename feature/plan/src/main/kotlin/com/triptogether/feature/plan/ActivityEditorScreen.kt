package com.triptogether.feature.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import kotlinx.datetime.LocalTime

/** S07 — activity form; place is free text until the Places API decision, attachments wait for Storage. */
@Composable
fun ActivityEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ActivityEditorEvent.Saved, ActivityEditorEvent.Deleted -> onDone()
                is ActivityEditorEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    ActivityEditorContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTitleChange = viewModel::onTitleChange,
        onPlaceChange = viewModel::onPlaceChange,
        onNoteChange = viewModel::onNoteChange,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityEditorContent(
    uiState: ActivityEditorUiState,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onStartTimeChange: (LocalTime?) -> Unit,
    onEndTimeChange: (LocalTime?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val titleRes =
                        if (uiState.isExisting) {
                            R.string.activity_editor_title_edit
                        } else {
                            R.string.activity_editor_title_new
                        }
                    Text(stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.day_plan_back),
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
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.activity_editor_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField(
                    label = stringResource(R.string.activity_editor_start),
                    value = uiState.startTime,
                    onChange = onStartTimeChange,
                    modifier = Modifier.weight(1f),
                )
                TimeField(
                    label = stringResource(R.string.activity_editor_end),
                    value = uiState.endTime,
                    onChange = onEndTimeChange,
                    modifier = Modifier.weight(1f),
                )
            }
            if (uiState.isTimeRangeInvalid) {
                Text(
                    text = stringResource(R.string.activity_editor_time_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = uiState.placeName,
                onValueChange = onPlaceChange,
                label = { Text(stringResource(R.string.activity_editor_place)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.activity_editor_note)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(stringResource(R.string.activity_editor_save))
                }
            }
            if (uiState.isExisting) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.activity_editor_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.activity_editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.activity_editor_delete_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.activity_editor_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.activity_editor_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    value: LocalTime?,
    onChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Text(text = value?.let { formatTime(it) } ?: label)
    }
    if (showPicker) {
        val pickerState =
            rememberTimePickerState(
                initialHour = value?.hour ?: DEFAULT_PICKER_HOUR,
                initialMinute = value?.minute ?: 0,
                is24Hour = true,
            )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(LocalTime(pickerState.hour, pickerState.minute))
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.activity_editor_time_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onChange(null)
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.activity_editor_time_clear))
                }
            },
        )
    }
}

private const val DEFAULT_PICKER_HOUR = 9

private fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

@Preview(showBackground = true)
@Composable
private fun ActivityEditorContentPreview() {
    TripTogetherTheme {
        ActivityEditorContent(
            uiState =
                ActivityEditorUiState(
                    title = "เช็คอินโรงแรม",
                    startTime = LocalTime(14, 0),
                    isExisting = true,
                ),
            snackbarHostState = SnackbarHostState(),
            onTitleChange = {},
            onPlaceChange = {},
            onNoteChange = {},
            onStartTimeChange = {},
            onEndTimeChange = {},
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}
