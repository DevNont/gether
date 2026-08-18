package com.triptogether.feature.auth

import androidx.annotation.StringRes

data class SignInUiState(
    val isLoading: Boolean = false,
)

sealed interface SignInEvent {
    data class Error(
        @StringRes val messageResId: Int,
    ) : SignInEvent
}
