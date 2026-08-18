package com.triptogether

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.ui.theme.TripTogetherTheme
import com.triptogether.feature.auth.SignInScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** Invite code from a deep link, held until the user is signed in (S01 spec). */
    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInviteCode = intent?.extractInviteCode()
        setContent {
            TripTogetherTheme {
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                when (authState) {
                    AuthUiState.Loading -> LoadingScreen()
                    AuthUiState.SignedOut -> SignInScreen()
                    is AuthUiState.SignedIn ->
                        AppNavHost(
                            pendingInviteCode = pendingInviteCode,
                            onInviteConsumed = { pendingInviteCode = null },
                        )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.extractInviteCode()?.let { pendingInviteCode = it }
    }
}

/** Parses triptogether://join/{code} and https://triptogether.app/join/{code}. */
private fun Intent.extractInviteCode(): String? {
    val uri = data ?: return null
    val code =
        when {
            uri.scheme == "triptogether" && uri.host == "join" -> uri.pathSegments.firstOrNull()
            uri.scheme == "https" && uri.host == "triptogether.app" &&
                uri.pathSegments.firstOrNull() == "join" -> uri.pathSegments.getOrNull(1)
            else -> null
        }
    return code?.uppercase()?.takeIf { it.isNotBlank() }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
