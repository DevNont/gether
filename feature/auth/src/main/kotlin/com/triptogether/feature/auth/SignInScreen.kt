package com.triptogether.feature.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.triptogether.core.domain.repository.AuthUiHost
import com.triptogether.core.ui.theme.TripTogetherTheme
import kotlinx.coroutines.launch

/** S01 — LINE sign-in (browser OIDC flow) or continue without an account. */
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
            onLineSignInClick = {
                val host = context.findAuthUiHost()
                if (host != null) viewModel.onLineSignIn(host) else viewModel.onCredentialFlowFailed()
            },
            onGoogleSignInClick = {
                scope.launch {
                    requestGoogleIdToken(
                        context = context,
                        serverClientId = viewModel.googleServerClientId,
                        onToken = viewModel::onGoogleIdToken,
                        onFailure = viewModel::onGoogleFlowFailed,
                    )
                }
            },
            onAnonymousSignIn = viewModel::onAnonymousSignIn,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** Walks the context chain to the activity, which implements AuthUiHost. */
internal fun Context.findAuthUiHost(): AuthUiHost? {
    var current: Context? = this
    while (current != null) {
        if (current is AuthUiHost) return current
        current = (current as? android.content.ContextWrapper)?.baseContext
    }
    return null
}

@Composable
private fun SignInContent(
    isLoading: Boolean,
    onLineSignInClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onAnonymousSignIn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNameEntry by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_app_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        when {
            isLoading -> CircularProgressIndicator()
            !showNameEntry -> {
                Button(
                    onClick = onLineSignInClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LineGreen),
                    modifier = Modifier.fillMaxWidth().testTag("sign_in_line"),
                ) {
                    Text(text = stringResource(R.string.auth_sign_in_with_line))
                }
                Button(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier.fillMaxWidth().testTag("sign_in_google"),
                ) {
                    Text(text = stringResource(R.string.auth_sign_in_with_google))
                }
                TextButton(
                    onClick = { showNameEntry = true },
                    modifier = Modifier.testTag("sign_in_anonymous"),
                ) {
                    Text(text = stringResource(R.string.auth_continue_without_account))
                }
            }
            else -> {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.auth_anon_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("anon_name"),
                )
                Text(
                    text = stringResource(R.string.auth_anon_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onAnonymousSignIn(nameInput.trim()) },
                    enabled = nameInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("anon_confirm"),
                ) {
                    Text(text = stringResource(R.string.auth_anon_confirm))
                }
                TextButton(onClick = { showNameEntry = false }) {
                    Text(text = stringResource(R.string.auth_anon_back))
                }
            }
        }
    }
}

/** LINE brand green. */
private val LineGreen = Color(0xFF06C755)

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
        SignInContent(
            isLoading = false,
            onLineSignInClick = {},
            onGoogleSignInClick = {},
            onAnonymousSignIn = {},
        )
    }
}
