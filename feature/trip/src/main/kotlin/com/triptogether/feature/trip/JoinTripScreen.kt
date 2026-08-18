package com.triptogether.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.model.INVITE_CODE_LENGTH
import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S04 — six-slot invite code entry with trip-name preview before confirming the join. */
@Composable
fun JoinTripScreen(
    onJoined: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialCode: String? = null,
    viewModel: JoinTripViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialCode) {
        if (!initialCode.isNullOrBlank()) viewModel.onCodeChange(initialCode)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinTripEvent.Joined -> onJoined(event.tripId)
                is JoinTripEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    JoinTripContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onCodeChange = viewModel::onCodeChange,
        onConfirm = viewModel::confirmJoin,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinTripContent(
    uiState: JoinTripUiState,
    snackbarHostState: SnackbarHostState,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_trip_title)) },
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
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.join_trip_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            CodeInput(code = uiState.code, onCodeChange = onCodeChange)
            when {
                uiState.isLookingUp -> CircularProgressIndicator()
                uiState.notFound ->
                    Text(
                        text = stringResource(R.string.join_trip_not_found),
                        color = MaterialTheme.colorScheme.error,
                    )
                uiState.preview != null ->
                    PreviewCard(
                        preview = uiState.preview,
                        isJoining = uiState.isJoining,
                        onConfirm = onConfirm,
                    )
            }
        }
    }
}

@Composable
private fun CodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        modifier = modifier,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(INVITE_CODE_LENGTH) { index ->
                    CodeSlot(char = code.getOrNull(index), focused = index == code.length)
                }
            }
        },
    )
}

@Composable
private fun CodeSlot(
    char: Char?,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier =
            modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString() ?: "",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun PreviewCard(
    preview: InvitePreview,
    isJoining: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = preview.tripName,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onConfirm, enabled = !isJoining) {
                if (isJoining) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Text(stringResource(R.string.join_trip_confirm))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinTripContentPreview() {
    TripTogetherTheme {
        JoinTripContent(
            uiState =
                JoinTripUiState(
                    code = "AB23CD",
                    preview = InvitePreview(tripId = "t1", tripName = "ทริปเชียงใหม่"),
                ),
            snackbarHostState = SnackbarHostState(),
            onCodeChange = {},
            onConfirm = {},
            onBack = {},
        )
    }
}
