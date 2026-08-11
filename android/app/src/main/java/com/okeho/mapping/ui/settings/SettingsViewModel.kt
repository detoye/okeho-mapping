package com.okeho.mapping.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okeho.mapping.data.local.OkehoDatabase
import com.okeho.mapping.data.remote.AuthManager
import com.okeho.mapping.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val name: String? = null,
    val email: String? = null,
    val pendingCount: Int = 0,
    val isSigningOut: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val db: OkehoDatabase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Refresh account details + unsynced count; called when the screen is shown. */
    fun refresh() {
        viewModelScope.launch {
            val pending = db.captureDao().countUnsynced() + db.streetDao().countUnsynced()
            _uiState.value = _uiState.value.copy(
                name = authManager.currentUserName,
                email = authManager.currentUserEmail,
                pendingCount = pending
            )
        }
    }

    /**
     * Signs out and wipes local data. The wipe-before-signout ordering lives in
     * [SignOutUseCase] because the drawer offers this too. The auth gate in
     * MainActivity reacts to sessionStatus, so there is no navigation here.
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true, message = null)
            signOutUseCase().onFailure {
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    message = "Sign out failed: ${it.message}"
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
