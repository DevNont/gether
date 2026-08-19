package com.triptogether.feature.plan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.ActivityType
import com.triptogether.core.domain.model.DayPlan
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter

/** S06 — horizontal day bar + vertical timeline with a per-day realtime listener. */
@Composable
fun DayPlanScreen(
    onAddActivity: (String) -> Unit,
    onEditActivity: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayPlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DayPlanContent(
        uiState = uiState,
        onSelectDay = viewModel::selectDay,
        onAddActivity = onAddActivity,
        onEditActivity = onEditActivity,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPlanContent(
    uiState: DayPlanUiState,
    onSelectDay: (String) -> Unit,
    onAddActivity: (String) -> Unit,
    onEditActivity: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.day_plan_title)) },
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
        floatingActionButton = {
            val dayId = uiState.selectedDayId
            if (dayId != null) {
                FloatingActionButton(onClick = { onAddActivity(dayId) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.day_plan_add),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            DayBar(
                days = uiState.days,
                selectedDayId = uiState.selectedDayId,
                onSelectDay = onSelectDay,
            )
            when {
                uiState.isLoading ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                uiState.isDayEmpty -> EmptyDay()
                else ->
                    Timeline(
                        stays = uiState.stays,
                        activities = uiState.timeline,
                        onEditActivity = { activityId ->
                            uiState.selectedDayId?.let { onEditActivity(it, activityId) }
                        },
                    )
            }
        }
    }
}

@Composable
private fun DayBar(
    days: List<DayPlan>,
    selectedDayId: String?,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(days, key = { it.id }) { day ->
            DayChip(
                day = day,
                selected = day.id == selectedDayId,
                isToday = day.date == today,
                onClick = { onSelectDay(day.id) },
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DayPlan,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = day.date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("E d MMM")))
                if (isToday) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun EmptyDay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.day_plan_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.day_plan_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Timeline(
    stays: List<Activity>,
    activities: List<Activity>,
    onEditActivity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        // The day's stay sits pinned on top — it frames the whole day.
        items(stays, key = { "stay-${it.id}" }) { stay ->
            StayCard(activity = stay, onClick = { onEditActivity(stay.id) })
        }
        items(activities, key = { it.id }) { activity ->
            ActivityRow(activity = activity, onClick = { onEditActivity(activity.id) })
        }
    }
}

@Composable
private fun StayCard(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Hotel,
                contentDescription = stringResource(R.string.activity_type_stay),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.day_plan_stay_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(text = activity.title, style = MaterialTheme.typography.titleSmall)
            }
            if (activity.placeName != null) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.size(20.dp).clickable { openInMaps(context, activity) },
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = activity.startTime?.toShortString() ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp).padding(top = 16.dp),
        )
        Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (activity.type == ActivityType.FOOD) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = stringResource(R.string.activity_type_food),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(text = activity.title, style = MaterialTheme.typography.titleSmall)
                    }
                    if (activity.attachments.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = stringResource(R.string.day_plan_has_attachments),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (activity.placeName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { openInMaps(context, activity) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = activity.placeName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                // Poll badge arrives with S13 voting in M6.
            }
        }
    }
}

private fun LocalTime.toShortString(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private fun openInMaps(
    context: Context,
    activity: Activity,
) {
    // Try the geo: URI first (opens a maps app with a pin), then fall back to the https Maps URL
    // which any browser handles. resolveActivity is unreliable on API 30+ (package visibility),
    // so launch directly and catch instead.
    val hasCoords = activity.lat != null && activity.lng != null
    val query = if (hasCoords) "${activity.lat},${activity.lng}" else activity.placeName.orEmpty()
    if (hasCoords) {
        val geo = Uri.parse("geo:${activity.lat},${activity.lng}?q=${Uri.encode(activity.placeName)}")
        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }.isSuccess) return
    }
    val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, web)) }
}

@Preview(showBackground = true)
@Composable
private fun DayPlanContentPreview() {
    val day =
        DayPlan(
            id = "2026-12-05",
            date = LocalDate(2026, 12, 5),
            activities =
                listOf(
                    Activity(
                        id = "a1",
                        title = "เช็คอินโรงแรม",
                        startTime = LocalTime(14, 0),
                        placeName = "โรงแรมช้างเผือก",
                        createdBy = "m1",
                    ),
                    Activity(id = "a2", title = "หาของกินแถวนิมมาน", createdBy = "m1"),
                ),
        )
    TripTogetherTheme {
        DayPlanContent(
            uiState =
                DayPlanUiState(
                    isLoading = false,
                    days = listOf(day),
                    selectedDayId = day.id,
                    selectedDay = day,
                ),
            onSelectDay = {},
            onAddActivity = {},
            onEditActivity = { _, _ -> },
            onBack = {},
        )
    }
}
