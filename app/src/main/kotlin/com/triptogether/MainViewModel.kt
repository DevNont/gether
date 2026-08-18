package com.triptogether

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState

    data object SignedOut : AuthUiState

    data class SignedIn(val user: User) : AuthUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val authState: StateFlow<AuthUiState> =
            authRepository.observeAuthState()
                .map { user -> if (user == null) AuthUiState.SignedOut else AuthUiState.SignedIn(user) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = AuthUiState.Loading,
                )

        fun signOut() {
            viewModelScope.launch { authRepository.signOut() }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
