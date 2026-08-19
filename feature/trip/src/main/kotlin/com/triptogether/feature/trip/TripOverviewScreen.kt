package com.triptogether.feature.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

/** S05 — trip header with countdown, members + invite share, shared note, owner menu. */
@Composable
fun TripOverviewScreen(
    onOpenPlan: (String) -> Unit,
    onOpenExpenses: (String) -> Unit,
    onOpenSettlement: (String) -> Unit,
    onOpenChecklist: (String) -> Unit,
    onOpenPolls: (String) -> Unit,
    onEditTrip: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripOverviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var inviteTrip by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripOverviewEvent.Message ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
                TripOverviewEvent.Deleted -> onDeleted()
            }
        }
    }

    TripOverviewContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNoteChange = viewModel::onNoteChange,
        onSaveNote = viewModel::saveNote,
        onArchive = viewModel::archiveTrip,
        onCloseInvite = viewModel::closeInvite,
        onAddGuest = viewModel::addGuestMember,
        onEditTrip = onEditTrip,
        onDeleteTrip = viewModel::deleteTrip,
        onShareInvite = { trip -> inviteTrip = trip },
        onOpenPlan = onOpenPlan,
        onOpenExpenses = onOpenExpenses,
        onOpenSettlement = onOpenSettlement,
        onOpenChecklist = onOpenChecklist,
        onOpenPolls = onOpenPolls,
        onBack = onBack,
        modifier = modifier,
    )

    inviteTrip?.let { trip ->
        InviteDialog(
            trip = trip,
            onShare = { shareInvite(context, trip) },
            onDismiss = { inviteTrip = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripOverviewContent(
    uiState: TripOverviewUiState,
    snackbarHostState: SnackbarHostState,
    onNoteChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onArchive: () -> Unit,
    onCloseInvite: () -> Unit,
    onAddGuest: (String) -> Unit,
    onEditTrip: (String) -> Unit,
    onDeleteTrip: () -> Unit,
    onShareInvite: (Trip) -> Unit,
    onOpenPlan: (String) -> Unit,
    onOpenExpenses: (String) -> Unit,
    onOpenSettlement: (String) -> Unit,
    onOpenChecklist: (String) -> Unit,
    onOpenPolls: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.create_trip_back),
                        )
                    }
                },
                actions = {
                    if (uiState.isOwner) {
                        OwnerMenu(
                            onEdit = { uiState.trip?.let { onEditTrip(it.id) } },
                            onArchive = onArchive,
                            onCloseInvite = onCloseInvite,
                            onDelete = onDeleteTrip,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val trip = uiState.trip
        if (trip == null) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(32.dp))
        } else {
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(trip = trip)
                LinkCard(
                    icon = Icons.Default.Map,
                    label = stringResource(R.string.overview_open_plan),
                    onClick = { onOpenPlan(trip.id) },
                )
                LinkCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    label = stringResource(R.string.overview_open_expenses),
                    onClick = { onOpenExpenses(trip.id) },
                )
                LinkCard(
                    icon = Icons.Default.Checklist,
                    label = stringResource(R.string.overview_open_checklist),
                    onClick = { onOpenChecklist(trip.id) },
                )
                LinkCard(
                    icon = Icons.Default.HowToVote,
                    label = stringResource(R.string.overview_open_polls),
                    onClick = { onOpenPolls(trip.id) },
                )
                MembersCard(
                    members = uiState.members,
                    onShareInvite = { onShareInvite(trip) },
                    onAddGuest = onAddGuest,
                )
                MoneySummaryCard(onClick = { onOpenSettlement(trip.id) })
                NoteCard(
                    note = uiState.noteDraft,
                    changed = uiState.noteChanged,
                    saving = uiState.isSavingNote,
                    onNoteChange = onNoteChange,
                    onSave = onSaveNote,
                )
            }
        }
    }
}

@Composable
private fun OwnerMenu(
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onCloseInvite: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.overview_menu),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.overview_edit_trip)) },
            onClick = {
                expanded = false
                onEdit()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.overview_close_invite)) },
            onClick = {
                expanded = false
                onCloseInvite()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.overview_archive)) },
            onClick = {
                expanded = false
                onArchive()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.overview_delete_trip),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                expanded = false
                showDeleteConfirm = true
            },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.overview_delete_confirm_title)) },
            text = { Text(stringResource(R.string.overview_delete_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.overview_delete_trip),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.overview_guest_cancel))
                }
            },
        )
    }
}

@Composable
private fun HeaderCard(
    trip: Trip,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = trip.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = formatDateRange(trip.startDate, trip.endDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CountdownText(trip = trip)
        }
    }
}

@Composable
private fun CountdownText(
    trip: Trip,
    modifier: Modifier = Modifier,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val text =
        when {
            today < trip.startDate ->
                stringResource(R.string.overview_countdown_days, today.daysUntil(trip.startDate))
            today > trip.endDate -> stringResource(R.string.overview_countdown_finished)
            else ->
                stringResource(
                    R.string.overview_countdown_day_of_trip,
                    trip.startDate.daysUntil(today) + 1,
                )
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun MembersCard(
    members: List<Member>,
    onShareInvite: () -> Unit,
    onAddGuest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGuestDialog by remember { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.overview_members, members.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onShareInvite) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Text(
                        text = stringResource(R.string.overview_invite),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            members.forEach { member ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MemberAvatar(member = member)
                    Text(text = member.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (member.isGuest) {
                        Text(
                            text = stringResource(R.string.overview_guest_tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = { showGuestDialog = true }) {
                Text(stringResource(R.string.overview_add_guest))
            }
        }
    }
    if (showGuestDialog) {
        AddGuestDialog(
            onAdd = {
                onAddGuest(it)
                showGuestDialog = false
            },
            onDismiss = { showGuestDialog = false },
        )
    }
}

@Composable
private fun AddGuestDialog(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overview_add_guest)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.overview_guest_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name) }) {
                Text(stringResource(R.string.overview_add_guest_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.overview_guest_cancel))
            }
        },
    )
}

@Composable
private fun LinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Tap opens S11 Settlement; inline totals land once the overview observes expenses. */
@Composable
private fun MoneySummaryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.overview_money_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.overview_money_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: String,
    changed: Boolean,
    saving: Boolean,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.overview_note_title),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text(stringResource(R.string.overview_note_placeholder)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (changed) {
                TextButton(onClick = onSave, enabled = !saving) {
                    Text(stringResource(R.string.overview_note_save))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripOverviewContentPreview() {
    TripTogetherTheme {
        TripOverviewContent(
            uiState =
                TripOverviewUiState(
                    isLoading = false,
                    trip =
                        Trip(
                            id = "t1",
                            name = "ทริปเชียงใหม่",
                            startDate = LocalDate(2026, 12, 5),
                            endDate = LocalDate(2026, 12, 7),
                            ownerId = "u1",
                            inviteCode = "AB23CD",
                        ),
                    members = listOf(Member(id = "m1", userId = "u1", displayName = "สมชาย")),
                    isOwner = true,
                ),
            snackbarHostState = SnackbarHostState(),
            onNoteChange = {},
            onSaveNote = {},
            onArchive = {},
            onCloseInvite = {},
            onAddGuest = {},
            onEditTrip = {},
            onDeleteTrip = {},
            onShareInvite = {},
            onOpenPlan = {},
            onOpenExpenses = {},
            onOpenSettlement = {},
            onOpenChecklist = {},
            onOpenPolls = {},
            onBack = {},
        )
    }
}
