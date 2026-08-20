package com.triptogether.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triptogether.core.domain.model.User
import com.triptogether.core.domain.repository.AuthRepository
import com.triptogether.core.domain.repository.AuthUiHost
import com.triptogether.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PHONE_LENGTH = 10
private const val CITIZEN_ID_LENGTH = 13

data class SettingsUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val displayNameDraft: String = "",
    val promptpayDraft: String = "",
    val touched: Boolean = false,
    val isSaving: Boolean = false,
    /** True while the session is an unlinked device-local account — shows the "link LINE" row. */
    val isAnonymous: Boolean = false,
    val linkedWithLine: Boolean = false,
    val linkedWithGoogle: Boolean = false,
) {
    /** PromptPay must be a 10-digit phone (leading 0) or a 13-digit citizen id (S14 spec). */
    val isPromptpayInvalid: Boolean
        get() {
            if (promptpayDraft.isBlank()) return false
            val digits = promptpayDraft.filter { it.isDigit() }
            return !(
                digits.length == CITIZEN_ID_LENGTH ||
                    (digits.length == PHONE_LENGTH && digits.startsWith("0"))
            )
        }

    val canSave: Boolean
        get() = touched && displayNameDraft.isNotBlank() && !isPromptpayInvalid && !isSaving
}

sealed interface SettingsEvent {
    data class Message(
        @StringRes val messageResId: Int,
    ) : SettingsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private data class Draft(
            val displayName: String = "",
            val promptpay: String = "",
            val touched: Boolean = false,
            val saving: Boolean = false,
        )

        private val draft = MutableStateFlow(Draft())

        private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
        val events: Flow<SettingsEvent> = _events.receiveAsFlow()

        val uiState: StateFlow<SettingsUiState> =
            authRepository.observeAuthState()
                .filterNotNull()
                .flatMapLatest { authUser ->
                    userRepository.observeUser(authUser.id).map { authUser to it }
                }
                .combine(draft) { (authUser, user), draft ->
                    SettingsUiState(
                        isLoading = user == null,
                        user = user,
                        displayNameDraft = if (draft.touched) draft.displayName else user?.displayName.orEmpty(),
                        promptpayDraft = if (draft.touched) draft.promptpay else user?.promptpayId.orEmpty(),
                        touched = draft.touched,
                        isSaving = draft.saving,
                        isAnonymous = authUser.isAnonymous,
                        linkedWithLine = authUser.linkedWithLine,
                        linkedWithGoogle = authUser.linkedWithGoogle,
                    )
                }.catch {
                    // A failed profile listener (e.g. PERMISSION_DENIED) must surface, not spin forever.
                    _events.send(SettingsEvent.Message(R.string.settings_error))
                    emit(SettingsUiState(isLoading = false))
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = SettingsUiState(),
                )

        fun onDisplayNameChange(value: String) {
            draft.update {
                it.copy(
                    displayName = value,
                    promptpay = uiState.value.promptpayDraft,
                    touched = true,
                )
            }
        }

        fun onPromptpayChange(value: String) {
            draft.update {
                it.copy(
                    displayName = uiState.value.displayNameDraft,
                    promptpay = value,
                    touched = true,
                )
            }
        }

        fun save() {
            val state = uiState.value
            val user = state.user ?: return
            if (!state.canSave) return
            viewModelScope.launch {
                draft.update { it.copy(saving = true) }
                userRepository.updateProfile(
                    user.copy(
                        displayName = state.displayNameDraft.trim(),
                        promptpayId = state.promptpayDraft.trim().ifBlank { null },
                    ),
                )
                    .onSuccess {
                        draft.update { it.copy(touched = false, saving = false) }
                        _events.send(SettingsEvent.Message(R.string.settings_saved))
                    }
                    .onFailure {
                        draft.update { it.copy(saving = false) }
                        _events.send(SettingsEvent.Message(R.string.settings_error))
                    }
            }
        }

        fun signOut() {
            viewModelScope.launch { authRepository.signOut() }
        }

        /** Upgrades the anonymous account to LINE, keeping the same uid so trips stay attached. */
        fun linkLine(host: AuthUiHost) {
            viewModelScope.launch {
                authRepository.linkWithLine(host)
                    .onSuccess { _events.send(SettingsEvent.Message(R.string.settings_link_line_done)) }
                    .onFailure { _events.send(SettingsEvent.Message(R.string.settings_link_line_error)) }
            }
        }

        /** Detaches LINE (uid unchanged); the UI has already confirmed the consequences. */
        fun unlinkLine() {
            viewModelScope.launch {
                authRepository.unlinkLine()
                    .onSuccess { _events.send(SettingsEvent.Message(R.string.settings_unlink_line_done)) }
                    .onFailure { _events.send(SettingsEvent.Message(R.string.settings_unlink_line_error)) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
