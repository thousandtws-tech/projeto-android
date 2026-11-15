package com.sgp.cliente.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreferencesUiState(
    val loading: Boolean = true,
    val invoiceAlerts: Boolean = true,
    val ticketUpdates: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = false,
                    invoiceAlerts = settingsRepository.invoiceAlertsEnabled.first(),
                    ticketUpdates = settingsRepository.ticketUpdatesEnabled.first(),
                )
            }
        }
    }

    fun setInvoiceAlerts(enabled: Boolean) {
        _uiState.update { it.copy(invoiceAlerts = enabled) }
    }

    fun setTicketUpdates(enabled: Boolean) {
        _uiState.update { it.copy(ticketUpdates = enabled) }
    }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            settingsRepository.setNotificationPrefs(state.invoiceAlerts, state.ticketUpdates)
            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }
}
