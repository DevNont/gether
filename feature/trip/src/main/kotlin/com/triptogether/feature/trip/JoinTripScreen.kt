package com.triptogether.feature.trip

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.triptogether.core.domain.model.INVITE_CODE_LENGTH
import com.triptogether.core.domain.model.InvitePreview
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S04 — six-slot invite code entry (typed or QR-scanned) with trip-name preview before joining. */
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

    val scanLauncher =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            result.contents?.let { scanned ->
                parseScannedInvite(scanned)?.let(viewModel::onCodeChange)
            }
        }
    val scanPrompt = stringResource(R.string.join_trip_scan_prompt)

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
        onScan = {
            scanLauncher.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt(scanPrompt)
                    .setBeepEnabled(false)
                    .setOrientationLocked(true),
            )
        },
        onConfirm = viewModel::confirmJoin,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Accepts the QR payloads we issue (deep link / https link) or a bare 6-char code. */
internal fun parseScannedInvite(text: String): String? {
    val trimmed = text.trim()
    val raw =
        when {
            trimmed.startsWith("triptogether://join/") -> trimmed.substringAfter("join/")
            trimmed.contains("triptogether.app/join/") -> trimmed.substringAfterLast("/")
            else -> trimmed
        }
    val code = raw.filter { it.isDigit() }
    return code.takeIf { it.length == INVITE_CODE_LENGTH }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinTripContent(
    uiState: JoinTripUiState,
    snackbarHostState: SnackbarHostState,
    onCodeChange: (String) -> Unit,
    onScan: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCodeEntry by remember { mutableStateOf(false) }

    fun backToMenu() {
        onCodeChange("")
        showCodeEntry = false
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (showCodeEntry) backToMenu() else onBack() }) {
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hero()
            if (showCodeEntry) {
                CodeEntry(
                    uiState = uiState,
                    onCodeChange = onCodeChange,
                    onConfirm = onConfirm,
                )
            } else {
                MethodMenu(
                    onScan = onScan,
                    onEnterCode = { showCodeEntry = true },
                )
            }
        }
    }
}

@Composable
private fun MethodMenu(
    onScan: () -> Unit,
    onEnterCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.join_trip_headline),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.join_trip_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MethodCard(
            icon = Icons.Default.QrCodeScanner,
            title = stringResource(R.string.join_trip_scan),
            desc = stringResource(R.string.join_method_scan_desc),
            onClick = onScan,
        )
        MethodCard(
            icon = Icons.Default.Dialpad,
            title = stringResource(R.string.join_method_code_title),
            desc = stringResource(R.string.join_method_code_desc),
            onClick = onEnterCode,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CodeEntry(
    uiState: JoinTripUiState,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.join_trip_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        CodeInput(code = uiState.code, onCodeChange = onCodeChange)
        Box(contentAlignment = Alignment.Center) {
            when {
                uiState.isLookingUp -> CircularProgressIndicator()
                uiState.notFound ->
                    Text(
                        text = stringResource(R.string.join_trip_not_found),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
            }
        }
        AnimatedVisibility(visible = uiState.preview != null) {
            uiState.preview?.let {
                PreviewCard(preview = it, isJoining = uiState.isJoining, onConfirm = onConfirm)
            }
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp),
        )
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
    val filled = char != null
    val borderColor =
        when {
            focused -> MaterialTheme.colorScheme.primary
            filled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    Box(
        modifier =
            modifier
                .size(width = 46.dp, height = 56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(if (focused || filled) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString() ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(R.string.join_trip_found),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = preview.tripName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfirm,
                enabled = !isJoining,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.join_trip_confirm),
                        style = MaterialTheme.typography.titleMedium,
                    )
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
            onScan = {},
            onConfirm = {},
            onBack = {},
        )
    }
}
