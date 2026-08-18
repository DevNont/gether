package com.triptogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptogether.core.ui.theme.TripTogetherTheme
import com.triptogether.feature.auth.SignInScreen
import com.triptogether.feature.trip.TripListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TripTogetherTheme {
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                when (authState) {
                    AuthUiState.Loading -> LoadingScreen()
                    AuthUiState.SignedOut -> SignInScreen()
                    is AuthUiState.SignedIn ->
                        TripListScreen(
                            // Wired to S03 CreateTrip and S04 JoinTrip in M2.4/M2.5.
                            onCreateTrip = {},
                            onJoinTrip = {},
                            onTripClick = {},
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
