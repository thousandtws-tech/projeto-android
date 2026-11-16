package com.sgp.cliente.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.common.result.onSuccess
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.contract.CheckServiceStatusUseCase
import com.sgp.cliente.domain.usecase.invoice.GetOpenInvoicesUseCase
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val session: CustomerSession? = null,
    val openInvoices: OpenInvoices = OpenInvoices(emptyList(), emptyList()),
    val serviceStatus: ServiceStatus? = null,
    val error: AppError? = null,
) {
    val contract: Contract? get() = session?.selectedContract
    val all: List<Invoice> get() = openInvoices.all
    val hasMultipleContracts: Boolean get() = (session?.contracts?.size ?: 0) > 1

    /** Most urgent open invoice — overdue first, then nearest due date. */
    val currentInvoice: Invoice?
        get() = openInvoices.overdue.firstOrNull() ?: openInvoices.upcoming.firstOrNull()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOpenInvoices: GetOpenInvoicesUseCase,
    private val checkServiceStatus: CheckServiceStatusUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var lastContractId: Int? = null
            observeSession().collect { state ->
                val session = (state as? SessionState.LoggedIn)?.session
                _uiState.update { it.copy(session = session) }
                // A contract switch elsewhere leaves the loaded invoices
                // pointing at the old contract — reload them right away.
                val contractId = session?.selectedContract?.id
                if (contractId != null && lastContractId != null && contractId != lastContractId) {
                    load(forceRefresh = true)
                    loadServiceStatus()
                }
                lastContractId = contractId ?: lastContractId
            }
        }
        load(forceRefresh = false)
        loadServiceStatus()
    }

    fun refresh() {
        load(forceRefresh = true)
        loadServiceStatus()
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            getOpenInvoices(forceRefresh).fold(
                onSuccess = { open ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, openInvoices = open)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, error = error)
                    }
                },
            )
        }
    }

    /** Best-effort health pill — silent on failure, status stays null. */
    private fun loadServiceStatus() {
        viewModelScope.launch {
            checkServiceStatus()
                .onSuccess { status ->
                    _uiState.update { it.copy(serviceStatus = status) }
                }
        }
    }
}
