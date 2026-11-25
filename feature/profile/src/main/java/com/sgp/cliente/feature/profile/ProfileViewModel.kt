package com.sgp.cliente.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ChangePasswordUseCase
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PasswordChangeState {
    data object Idle : PasswordChangeState
    data object Loading : PasswordChangeState
    data object Success : PasswordChangeState
    data class Error(val error: AppError) : PasswordChangeState
}

data class ProfileUiState(
    val session: CustomerSession? = null,
    val passwordChange: PasswordChangeState = PasswordChangeState.Idle,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                _uiState.value = ProfileUiState(
                    session = (state as? SessionState.LoggedIn)?.session,
                )
            }
        }
    }

    // No post-logout event: navigation is driven by the session observer in
    // SgpApp. Emitting to a SharedFlow here used to suspend on the main
    // thread while the store was being torn down, starving the event loop.
    fun logout() {
        AppLog.i(TAG, "logout requested")
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        val validationError = when {
            new.isBlank() -> "Informe a nova senha."
            new.length < MIN_PASSWORD_LENGTH -> "A nova senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."
            new != confirm -> "As senhas não coincidem."
            else -> null
        }
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(
                passwordChange = PasswordChangeState.Error(AppError.Validation(validationError)),
            )
            return
        }
        _uiState.value = _uiState.value.copy(passwordChange = PasswordChangeState.Loading)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                passwordChange = changePasswordUseCase(current, new).fold(
                    onSuccess = { PasswordChangeState.Success },
                    onFailure = { PasswordChangeState.Error(it) },
                ),
            )
        }
    }

    /** Clears the one-shot result once the sheet has reacted to it. */
    fun resetPasswordChange() {
        _uiState.value = _uiState.value.copy(passwordChange = PasswordChangeState.Idle)
    }

    private companion object {
        const val TAG = "feature.profile"
        const val MIN_PASSWORD_LENGTH = 4
    }
}
