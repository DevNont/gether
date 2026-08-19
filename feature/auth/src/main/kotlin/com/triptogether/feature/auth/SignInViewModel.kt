package com.triptogether.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.AuthUiHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SignInUiState())
        val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

        private val _events = Channel<SignInEvent>(Channel.BUFFERED)
        val events: Flow<SignInEvent> = _events.receiveAsFlow()

        /** Auth state flow in the app layer flips the UI on success, so only failures emit events here. */
        fun onCredentialFlowFailed() {
            viewModelScope.launch { _events.send(SignInEvent.Error(R.string.auth_line_error)) }
        }

        fun onLineSignIn(host: AuthUiHost) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                authRepository.signInWithLine(host)
                    .onFailure { _events.send(SignInEvent.Error(R.string.auth_line_error)) }
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        fun onAnonymousSignIn(displayName: String) {
            if (displayName.isBlank()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                authRepository.signInAnonymously(displayName)
                    .onFailure { _events.send(SignInEvent.Error(R.string.auth_anon_error)) }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
