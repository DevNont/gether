package com.triptogether.feature.auth

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.triptogether.core.domain.model.User
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S14 — profile (name, photo read-only, PromptPay with validation) and sign out. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    SettingsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onPromptpayChange = viewModel::onPromptpayChange,
        onSave = viewModel::save,
        onSignOut = viewModel::signOut,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onDisplayNameChange: (String) -> Unit,
    onPromptpayChange: (String) -> Unit,
    onSave: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.user?.photoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                )
            }
            OutlinedTextField(
                value = uiState.displayNameDraft,
                onValueChange = onDisplayNameChange,
                label = { Text(stringResource(R.string.settings_display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.promptpayDraft,
                onValueChange = onPromptpayChange,
                label = { Text(stringResource(R.string.settings_promptpay)) },
                supportingText = {
                    if (uiState.isPromptpayInvalid) {
                        Text(
                            text = stringResource(R.string.settings_promptpay_invalid),
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(stringResource(R.string.settings_promptpay_hint))
                    }
                },
                isError = uiState.isPromptpayInvalid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save))
            }
            LanguageSelector()
            // Notification toggles and account deletion arrive with FCM (M6.1) / account tooling.
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_sign_out),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** App language — Thai is the default set at startup; the choice persists via AppCompat. */
@Composable
private fun LanguageSelector(modifier: Modifier = Modifier) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = current.startsWith("th") || current.isEmpty(),
                onClick = {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
                },
                label = { Text(stringResource(R.string.settings_language_th)) },
            )
            FilterChip(
                selected = current.startsWith("en"),
                onClick = {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                },
                label = { Text(stringResource(R.string.settings_language_en)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    TripTogetherTheme {
        SettingsContent(
            uiState =
                SettingsUiState(
                    isLoading = false,
                    user = User(id = "u1", displayName = "สมชาย"),
                    displayNameDraft = "สมชาย",
                    promptpayDraft = "0812345678",
                ),
            snackbarHostState = SnackbarHostState(),
            onDisplayNameChange = {},
            onPromptpayChange = {},
            onSave = {},
            onSignOut = {},
            onBack = {},
        )
    }
}
