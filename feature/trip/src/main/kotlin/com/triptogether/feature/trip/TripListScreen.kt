package com.triptogether.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.Trip
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.LocalDate

/** S02 — trips in two groups (upcoming / finished) with empty state, FAB to create, join via header. */
@Composable
fun TripListScreen(
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onTripClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    devMode: Boolean = false,
    viewModel: TripListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripListContent(
        uiState = uiState,
        onCreateTrip = onCreateTrip,
        onJoinTrip = onJoinTrip,
        onTripClick = onTripClick,
        onOpenSettings = onOpenSettings,
        onSeedDemo = if (devMode) viewModel::seedDemoTrip else null,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripListContent(
    uiState: TripListUiState,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onTripClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSeedDemo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_list_title)) },
                actions = {
                    if (onSeedDemo != null) {
                        IconButton(onClick = onSeedDemo) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = stringResource(R.string.trip_list_seed_demo),
                            )
                        }
                    }
                    IconButton(onClick = onJoinTrip) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = stringResource(R.string.trip_list_join),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.trip_list_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.trip_list_create),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.isEmpty -> EmptyState(Modifier.padding(innerPadding))
            else ->
                TripGroups(
                    uiState = uiState,
                    onTripClick = onTripClick,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.trip_list_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.trip_list_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripGroups(
    uiState: TripListUiState,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.upcoming.isNotEmpty()) {
            item { GroupHeader(stringResource(R.string.trip_list_group_upcoming)) }
            items(uiState.upcoming, key = { it.trip.id }) { card ->
                TripCard(card = card, onClick = { onTripClick(card.trip.id) })
            }
        }
        if (uiState.past.isNotEmpty()) {
            item { GroupHeader(stringResource(R.string.trip_list_group_past)) }
            items(uiState.past, key = { it.trip.id }) { card ->
                TripCard(card = card, onClick = { onTripClick(card.trip.id) })
            }
        }
    }
}

@Composable
private fun GroupHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun TripCard(
    card: TripCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = card.trip.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDateRange(card.trip.startDate, card.trip.endDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.members.isNotEmpty()) {
                AvatarStack(members = card.members)
            }
        }
    }
}

private const val MAX_AVATARS = 5

@Composable
private fun AvatarStack(
    members: List<Member>,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        members.take(MAX_AVATARS).forEachIndexed { index, member ->
            MemberAvatar(
                member = member,
                modifier = Modifier.offset(x = (-8 * index).dp),
            )
        }
        val overflow = members.size - MAX_AVATARS
        if (overflow > 0) {
            Box(
                modifier =
                    Modifier
                        .offset(x = (-8 * MAX_AVATARS).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.trip_list_avatar_overflow, overflow),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripListContentPreview() {
    val trip =
        Trip(
            id = "t1",
            name = "ทริปเชียงใหม่",
            startDate = LocalDate(2026, 12, 5),
            endDate = LocalDate(2026, 12, 7),
            ownerId = "u1",
            inviteCode = "AB23CD",
        )
    TripTogetherTheme {
        TripListContent(
            uiState =
                TripListUiState(
                    isLoading = false,
                    upcoming =
                        listOf(
                            TripCardUi(
                                trip = trip,
                                members =
                                    listOf(
                                        Member(id = "m1", userId = "u1", displayName = "สมชาย"),
                                        Member(id = "m2", userId = null, displayName = "น้องแมว"),
                                    ),
                            ),
                        ),
                ),
            onCreateTrip = {},
            onJoinTrip = {},
            onTripClick = {},
            onOpenSettings = {},
            onSeedDemo = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripListEmptyPreview() {
    TripTogetherTheme {
        TripListContent(
            uiState = TripListUiState(isLoading = false),
            onCreateTrip = {},
            onJoinTrip = {},
            onTripClick = {},
            onOpenSettings = {},
            onSeedDemo = null,
        )
    }
}
