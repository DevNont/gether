package com.triptogether.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.ui.theme.TripTogetherTheme

/** Settings > พร้อมเพย์ — the id behind the QR everyone pays you with (S11). */
@Composable
fun SettingsPromptpayScreen(
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

    SettingsPromptpayContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onPromptpayChange = viewModel::onPromptpayChange,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPromptpayContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onPromptpayChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_menu_promptpay)) },
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
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            if (uiState.touched) {
                Button(
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPromptpayContentPreview() {
    TripTogetherTheme {
        SettingsPromptpayContent(
            uiState = SettingsUiState(isLoading = false, promptpayDraft = "0812345678", touched = true),
            snackbarHostState = SnackbarHostState(),
            onPromptpayChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
