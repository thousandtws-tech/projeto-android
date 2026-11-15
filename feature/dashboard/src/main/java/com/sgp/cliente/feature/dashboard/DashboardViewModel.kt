package com.sgp.cliente.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
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
    val error: AppError? = null,
) {
    val contract: Contract? get() = session?.selectedContract
    val all: List<Invoice> get() = openInvoices.all
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOpenInvoices: GetOpenInvoicesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                _uiState.update {
                    it.copy(session = (state as? SessionState.LoggedIn)?.session)
                }
            }
        }
        load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

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
}
