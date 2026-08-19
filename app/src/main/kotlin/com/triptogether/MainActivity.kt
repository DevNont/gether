package com.triptogether

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.domain.repository.AuthUiHost
import com.triptogether.core.ui.theme.TripTogetherTheme
import com.triptogether.feature.auth.SignInScreen
import com.triptogether.notifications.Notifications
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not ComponentActivity) so setApplicationLocales recreates with the right locale.
// AuthUiHost lets the data layer launch browser sign-in flows (LINE) from this activity.
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AuthUiHost {
    private val viewModel: MainViewModel by viewModels()

    /** Invite code from a deep link, held until the user is signed in (S01 spec). */
    private var pendingInviteCode by mutableStateOf<String?>(null)

    /** Trip id from a tapped meetup notification, held until the nav graph consumes it. */
    private var pendingTripId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh launch — after a config change the same intent comes back
        // and would replay the deep link / notification navigation on every rotation.
        if (savedInstanceState == null) {
            pendingInviteCode = intent?.extractInviteCode()
            pendingTripId = intent?.getStringExtra(Notifications.EXTRA_TRIP_ID)
        }
        setContent {
            TripTogetherTheme {
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                Column {
                    // Thin non-blocking banner per docs/03 cross-cutting; Firestore keeps syncing later.
                    if (!isOnline) {
                        OfflineBanner()
                    }
                    when (authState) {
                        AuthUiState.Loading -> LoadingScreen()
                        AuthUiState.SignedOut -> SignInScreen()
                        is AuthUiState.SignedIn ->
                            AppNavHost(
                                pendingInviteCode = pendingInviteCode,
                                onInviteConsumed = { pendingInviteCode = null },
                                pendingTripId = pendingTripId,
                                onTripConsumed = { pendingTripId = null },
                            )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.extractInviteCode()?.let { pendingInviteCode = it }
        intent.getStringExtra(Notifications.EXTRA_TRIP_ID)?.let { pendingTripId = it }
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

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.offline_banner),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(vertical = 4.dp),
    )
}
