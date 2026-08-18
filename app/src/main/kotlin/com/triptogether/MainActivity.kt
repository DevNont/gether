package com.triptogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.ui.theme.TripTogetherTheme
import com.triptogether.feature.auth.SignInScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TripTogetherTheme {
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                when (val state = authState) {
                    AuthUiState.Loading -> LoadingScreen()
                    AuthUiState.SignedOut -> SignInScreen()
                    is AuthUiState.SignedIn ->
                        HomePlaceholderScreen(
                            displayName = state.user.displayName,
                            onSignOut = viewModel::signOut,
                        )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Temporary home until S02 TripList lands in M2.3. */
@Composable
private fun HomePlaceholderScreen(
    displayName: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.home_signed_in_as, displayName))
        Button(onClick = onSignOut) {
            Text(text = stringResource(R.string.home_sign_out))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePlaceholderScreenPreview() {
    TripTogetherTheme {
        HomePlaceholderScreen(displayName = "สมชาย", onSignOut = {})
    }
}
