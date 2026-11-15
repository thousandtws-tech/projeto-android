package com.sgp.cliente.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val loginError: String? = null,
    val passwordError: String? = null,
    val formError: AppError? = null,
) {
    val canSubmit: Boolean
        get() = !loading && login.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginChange(value: String) {
        _uiState.update {
            it.copy(login = value, loginError = null, formError = null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, formError = null)
        }
    }

    fun submit() {
        val state = _uiState.value
        val loginError =
            if (state.login.isBlank()) "Informe o CPF/CNPJ ou usuário" else null
        val passwordError = if (state.password.isNotBlank()) null else "Informe a senha"

        if (loginError != null || passwordError != null) {
            _uiState.update {
                it.copy(loginError = loginError, passwordError = passwordError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, formError = null) }
            loginUseCase(state.login, state.password).fold(
                onSuccess = {
                    // Navigation is driven by the session observer in SgpApp —
                    // no one-shot event needed here.
                    _uiState.update { s -> s.copy(loading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loading = false, formError = error) }
                },
            )
        }
    }
}
