package com.triptogether.feature.extras

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Meetup
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Trip meetups (appointments) with a per-meetup local reminder. */
@Composable
fun MeetupScreen(
    onBack: () -> Unit,
    onOpenEditor: (tripId: String, meetupId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tripId = viewModel.tripId
    val snackbarHostState = remember { SnackbarHostState() }

    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    // Ask for notification permission (Android 13+) once; a denial shows the inline banner instead.
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationsEnabled &&
            !permissionRequested
        ) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Re-check on resume so returning from the system settings clears (or shows) the banner.
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MeetupEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    MeetupContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showNotificationsOffBanner = !notificationsEnabled,
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onAdd = { onOpenEditor(tripId, null) },
        onOpen = { meetupId -> onOpenEditor(tripId, meetupId) },
        onBack = onBack,
        modifier = modifier,
    )
}

private fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetupContent(
    uiState: MeetupUiState,
    snackbarHostState: SnackbarHostState,
    showNotificationsOffBanner: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meetup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.extras_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.meetup_add))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (showNotificationsOffBanner) {
                NotificationsOffBanner(onOpenSettings = onOpenNotificationSettings)
            }
            when {
                uiState.isLoading ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                uiState.meetups.isEmpty() ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.meetup_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        items(uiState.meetups, key = { it.id }) { meetup ->
                            val isPast =
                                meetup.date < now.date ||
                                    (meetup.date == now.date && meetup.time < now.time)
                            MeetupCard(meetup = meetup, isPast = isPast, onClick = { onOpen(meetup.id) })
                        }
                    }
            }
        }
    }
}

/** Thin inline warning shown while notifications are off, so users know reminders will be silent. */
@Composable
private fun NotificationsOffBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.meetup_notifications_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.meetup_open_settings))
            }
        }
    }
}

@Composable
private fun MeetupCard(
    meetup: Meetup,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().alpha(if (isPast) 0.6f else 1f).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = meetup.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (isPast) {
                    Text(
                        text = stringResource(R.string.meetup_passed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatDateTime(meetup.date, meetup.time),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
            meetup.place?.takeIf { it.isNotBlank() }?.let { place ->
                IconLabel(icon = Icons.Default.Place, text = place)
            }
            IconLabel(
                icon = Icons.Default.Notifications,
                text = stringResource(reminderLabelRes(meetup.reminderMinutesBefore)),
            )
        }
    }
}

@Composable
private fun IconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun reminderLabelRes(minutes: Int): Int =
    when (minutes) {
        15 -> R.string.meetup_reminder_15
        30 -> R.string.meetup_reminder_30
        60 -> R.string.meetup_reminder_60
        else -> R.string.meetup_reminder_at_time
    }

internal fun formatDateTime(
    date: LocalDate,
    time: LocalTime,
): String {
    // Locale.getDefault() follows the per-app locale, so day names match the UI language.
    val d = date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("E d MMM yy", Locale.getDefault()))
    val t = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    return "$d · $t"
}

@Preview(showBackground = true)
@Composable
private fun MeetupContentPreview() {
    TripTogetherTheme {
        MeetupContent(
            uiState =
                MeetupUiState(
                    isLoading = false,
                    meetups =
                        listOf(
                            Meetup(
                                id = "m1",
                                title = "เจอกันที่ lobby",
                                place = "Hotel Lobby",
                                date = LocalDate(2026, 8, 20),
                                time = LocalTime(7, 0),
                                reminderMinutesBefore = 30,
                                createdBy = "m1",
                            ),
                        ),
                ),
            snackbarHostState = SnackbarHostState(),
            showNotificationsOffBanner = true,
            onOpenNotificationSettings = {},
            onAdd = {},
            onOpen = {},
            onBack = {},
        )
    }
}
