package com.sgp.cliente.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.repository.AuthRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerConfigUiState(
    val baseUrl: String = "",
    val checking: Boolean = false,
    val selectedCentral: String? = null,
    val error: AppError? = null,
    val fieldError: String? = null,
    val saved: Boolean = false,
)

/**
 * Persists the SGP instance override and discovers which Central App
 * (white-label tenant) the server exposes, storing its id + X-TOKEN —
 * the credentials every `/api/centralapp/` call depends on.
 */
@HiltViewModel
class ServerConfigViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerConfigUiState())
    val uiState: StateFlow<ServerConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settingsRepository.baseUrlOverride.first().orEmpty()
            _uiState.update { it.copy(baseUrl = current) }
        }
    }

    fun onUrlChange(value: String) {
        _uiState.update {
            it.copy(baseUrl = value.trim(), error = null, fieldError = null, saved = false)
        }
    }

    fun save() {
        val url = _uiState.value.baseUrl.trim()
        // BaseUrlProvider adds https:// when missing, so bare hosts are fine.
        if (url.any(Char::isWhitespace)) {
            _uiState.update { it.copy(fieldError = "A URL não pode conter espaços") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, error = null) }
            authRepository.discoverAndSelectCentral(url).fold(
                onSuccess = { central ->
                    settingsRepository.setBaseUrlOverride(url)
                    _uiState.update {
                        it.copy(
                            checking = false,
                            selectedCentral = central.name.ifBlank { "Central #${central.id}" },
                            saved = true,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(checking = false, error = error, fieldError = null)
                    }
                },
            )
        }
    }
}
