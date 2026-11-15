package com.sgp.cliente.feature.paymentpromises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.usecase.invoice.GetOpenInvoicesUseCase
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import com.sgp.cliente.domain.usecase.payment.RequestPaymentPromiseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentPromisesUiState(
    val loading: Boolean = true,
    val openInvoices: OpenInvoices = OpenInvoices(emptyList(), emptyList()),
    val requesting: Boolean = false,
    val result: PaymentPromise? = null,
    val error: AppError? = null,
    val actionError: AppError? = null,
)

@HiltViewModel
class PaymentPromisesViewModel @Inject constructor(
    private val getOpenInvoices: GetOpenInvoicesUseCase,
    private val requestPromise: RequestPaymentPromiseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentPromisesUiState())
    val uiState: StateFlow<PaymentPromisesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            getOpenInvoices().fold(
                onSuccess = { open ->
                    _uiState.update { it.copy(loading = false, openInvoices = open) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loading = false, error = error) }
                },
            )
        }
    }

    /** The SGP promise endpoint is contract-level — it applies to the selected contract. */
    fun request() {
        viewModelScope.launch {
            _uiState.update { it.copy(requesting = true, actionError = null) }
            requestPromise().fold(
                onSuccess = { result ->
                    _uiState.update { it.copy(requesting = false, result = result) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(requesting = false, actionError = error) }
                },
            )
        }
    }

    fun consumeResult() = _uiState.update { it.copy(result = null) }
    fun consumeActionError() = _uiState.update { it.copy(actionError = null) }
}
