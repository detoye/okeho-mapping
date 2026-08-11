package com.okeho.mapping.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okeho.mapping.data.remote.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

/**
 * Shared by the login and signup screens. Holding this state in a ViewModel
 * rather than in `remember` means it survives rotation, and keeps the auth
 * calls off the composable.
 *
 * Neither screen navigates on success: the gate in MainActivity observes
 * [AuthManager.sessionStatus] and swaps the graph. A success callback here
 * would be a second, racing navigation path.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    /** The library's own session flow; drives the gate in MainActivity. */
    val sessionStatus: StateFlow<SessionStatus> = authManager.sessionStatus

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Re-runs session restore after a NetworkError splash. */
    fun reloadSession() {
        viewModelScope.launch { authManager.reloadSession() }
    }

    /**
     * Sends a password reset email. The message shown is deliberately neutral:
     * Supabase succeeds whether or not the address has an account, and echoing
     * a difference would leak which emails are registered.
     */
    fun sendPasswordReset() {
        val trimmed = _uiState.value.email.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email address first")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, info = null)
            authManager.sendPasswordReset(trimmed)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                info = "If an account exists for $trimmed, a reset link is on its way."
            )
        }
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun updateConfirmPassword(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, error = null)
    }

    fun updateFullName(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value, error = null)
    }

    /** Clears transient state so a screen swap doesn't inherit the other's error. */
    fun resetTransient() {
        _uiState.value = _uiState.value.copy(isLoading = false, error = null, info = null)
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, info = null)
            authManager.signIn(state.email.trim(), state.password).fold(
                onSuccess = {
                    // The gate reacts to sessionStatus; nothing to do here.
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign in failed"
                    )
                }
            )
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.fullName.isBlank()) return
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Passwords do not match")
            return
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.value = state.copy(
                error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null, info = null)
            authManager.signUp(
                email = state.email.trim(),
                password = state.password,
                fullName = state.fullName.trim()
            ).fold(
                onSuccess = { hasSession ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        // No session means the project still requires email
                        // confirmation. Say so, rather than sitting on a screen
                        // that will never navigate.
                        info = if (hasSession) null
                        else "Account created. Check your email to confirm before signing in."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign up failed"
                    )
                }
            )
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
