package com.triptogether.feature.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.Poll
import com.triptogether.core.domain.model.PollOption
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S13 — polls with proportion bars, voter names, owner close; create dialog with 2-8 options. */
@Composable
fun PollsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PollsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PollsEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    PollsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onCreatePoll = viewModel::createPoll,
        onToggleVote = viewModel::toggleVote,
        onClosePoll = viewModel::closePoll,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollsContent(
    uiState: PollsUiState,
    snackbarHostState: SnackbarHostState,
    onCreatePoll: (String, List<String>, Boolean) -> Unit,
    onToggleVote: (Poll, PollOption) -> Unit,
    onClosePoll: (Poll) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.polls_title)) },
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
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.polls_create),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            uiState.polls.isEmpty() ->
                Box(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.polls_empty))
                }
            else ->
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    items(uiState.polls, key = { it.id }) { poll ->
                        PollCard(
                            poll = poll,
                            uiState = uiState,
                            onToggleVote = onToggleVote,
                            onClosePoll = onClosePoll,
                        )
                    }
                }
        }
    }

    if (showCreate) {
        CreatePollDialog(
            onCreate = { question, options, multi ->
                onCreatePoll(question, options, multi)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun PollCard(
    poll: Poll,
    uiState: PollsUiState,
    onToggleVote: (Poll, PollOption) -> Unit,
    onClosePoll: (Poll) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalVotes = poll.options.sumOf { it.voterMemberIds.size }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = poll.question, style = MaterialTheme.typography.titleMedium)
                if (uiState.canClose(poll)) {
                    TextButton(onClick = { onClosePoll(poll) }) {
                        Text(stringResource(R.string.polls_close))
                    }
                } else if (poll.closed) {
                    Text(
                        text = stringResource(R.string.polls_closed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            poll.options.forEach { option ->
                PollOptionRow(
                    option = option,
                    totalVotes = totalVotes,
                    voterNames = option.voterMemberIds.map { uiState.memberName(it) },
                    enabled = !poll.closed,
                    onClick = { onToggleVote(poll, option) },
                )
            }
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PollOption,
    totalVotes: Int,
    voterNames: List<String>,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (totalVotes == 0) 0f else option.voterMemberIds.size.toFloat() / totalVotes
    Column(modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = option.voterMemberIds.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (voterNames.isNotEmpty()) {
            Text(
                text = voterNames.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun CreatePollDialog(
    onCreate: (String, List<String>, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var multiChoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.polls_create)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text(stringResource(R.string.polls_question)) },
                    singleLine = true,
                )
                options.forEachIndexed { index, option ->
                    OutlinedTextField(
                        value = option,
                        onValueChange = { options[index] = it },
                        label = { Text(stringResource(R.string.polls_option_n, index + 1)) },
                        singleLine = true,
                    )
                }
                if (options.size < POLL_MAX_OPTIONS) {
                    TextButton(onClick = { options.add("") }) {
                        Text(stringResource(R.string.polls_add_option))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = multiChoice, onCheckedChange = { multiChoice = it })
                    Text(stringResource(R.string.polls_multi_choice))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= POLL_MIN_OPTIONS,
                onClick = { onCreate(question, options.toList(), multiChoice) },
            ) {
                Text(stringResource(R.string.polls_create_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.polls_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PollsContentPreview() {
    TripTogetherTheme {
        PollsContent(
            uiState =
                PollsUiState(
                    isLoading = false,
                    polls =
                        listOf(
                            Poll(
                                id = "p1",
                                question = "มื้อเย็นกินอะไรดี?",
                                options =
                                    listOf(
                                        PollOption(id = "o1", label = "หมูกระทะ", voterMemberIds = setOf("m1")),
                                        PollOption(id = "o2", label = "ชาบู"),
                                    ),
                                createdBy = "m1",
                            ),
                        ),
                    myMemberId = "m1",
                ),
            snackbarHostState = SnackbarHostState(),
            onCreatePoll = { _, _, _ -> },
            onToggleVote = { _, _ -> },
            onClosePoll = {},
            onBack = {},
        )
    }
}
