package com.triptogether.feature.auth

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.triptogether.core.domain.model.User
import com.triptogether.core.ui.theme.TripTogetherTheme

/** S14 — profile header, then three quiet sections: profile, money, language. One save, one exit. */
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
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(user = uiState.user)

            SettingsSection(title = stringResource(R.string.settings_section_profile)) {
                OutlinedTextField(
                    value = uiState.displayNameDraft,
                    onValueChange = onDisplayNameChange,
                    label = { Text(stringResource(R.string.settings_display_name)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_money)) {
                OutlinedTextField(
                    value = uiState.promptpayDraft,
                    onValueChange = onPromptpayChange,
                    label = { Text(stringResource(R.string.settings_promptpay)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.QrCode2, contentDescription = null) },
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
            }

            if (uiState.touched) {
                FilledTonalButton(
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }

            SettingsSection(title = stringResource(R.string.settings_language)) {
                LanguageSelector()
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.settings_sign_out),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Big avatar + name — who you are in every trip, front and center. */
@Composable
private fun ProfileHeader(
    user: User?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user?.displayName?.take(1) ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = user?.displayName.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

/** App language — Thai is the default set at startup; the choice persists via AppCompat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(modifier: Modifier = Modifier) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isThai = current.startsWith("th") || current.isEmpty()
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = isThai,
            onClick = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("th"))
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(R.string.settings_language_th))
        }
        SegmentedButton(
            selected = !isThai,
            onClick = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(R.string.settings_language_en))
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
                    touched = true,
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
