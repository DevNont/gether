package com.triptogether.feature.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.coroutines.launch

/** S01 — single Google sign-in button via Credential Manager. */
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(event.messageResId))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SignInContent(
            isLoading = uiState.isLoading,
            onSignInClick = {
                scope.launch {
                    requestGoogleIdToken(
                        context = context,
                        serverClientId = viewModel.googleServerClientId,
                        onToken = viewModel::onGoogleIdToken,
                        onFailure = viewModel::onCredentialFlowFailed,
                    )
                }
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SignInContent(
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_app_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onSignInClick) {
                Text(text = stringResource(R.string.auth_sign_in_with_google))
            }
        }
    }
}

private suspend fun requestGoogleIdToken(
    context: Context,
    serverClientId: String,
    onToken: (String) -> Unit,
    onFailure: () -> Unit,
) {
    val option =
        GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            onToken(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            onFailure()
        }
    } catch (_: GetCredentialException) {
        onFailure()
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInContentPreview() {
    TripTogetherTheme {
        SignInContent(isLoading = false, onSignInClick = {})
    }
}
