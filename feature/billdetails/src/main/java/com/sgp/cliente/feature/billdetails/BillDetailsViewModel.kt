package com.sgp.cliente.feature.billdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.usecase.invoice.GetBoletoUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoiceDetailsUseCase
import com.sgp.cliente.domain.usecase.invoice.SendInvoiceCopyUseCase
import com.sgp.cliente.domain.usecase.payment.RequestPaymentPromiseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillDetailsUiState(
    val loading: Boolean = true,
    val invoice: Invoice? = null,
    val sendingCopy: Boolean = false,
    val requestingPromise: Boolean = false,
    val sentCopyMessage: String? = null,
    val promise: PaymentPromise? = null,
    val boletoOpen: Boolean = false,
    val boleto: BoletoContent? = null,
    val boletoLoading: Boolean = false,
    val boletoError: AppError? = null,
    val error: AppError? = null,
    val actionError: AppError? = null,
)

@HiltViewModel
class BillDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetails: GetInvoiceDetailsUseCase,
    private val getBoleto: GetBoletoUseCase,
    private val sendCopy: SendInvoiceCopyUseCase,
    private val requestPaymentPromise: RequestPaymentPromiseUseCase,
) : ViewModel() {

    private var invoiceId: Int? = savedStateHandle["invoiceId"]

    private val _uiState = MutableStateFlow(BillDetailsUiState())
    val uiState: StateFlow<BillDetailsUiState> = _uiState.asStateFlow()

    init {
        if (invoiceId != null) load()
    }

    /** Opens an invoice — resets state and loads (sheet usage reuses the VM). */
    fun show(id: Int) {
        invoiceId = id
        _uiState.value = BillDetailsUiState()
        load()
    }

    fun load() {
        val id = invoiceId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            getDetails(id).fold(
                onSuccess = { invoice ->
                    _uiState.update { it.copy(loading = false, invoice = invoice) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loading = false, error = error) }
                },
            )
        }
    }

    /** Downloads the boleto PDF and opens the in-app viewer. */
    fun openBoleto() {
        val invoice = _uiState.value.invoice ?: return
        if (invoice.boletoPath == null) return
        if (_uiState.value.boletoLoading) return
        if (_uiState.value.boletoOpen && _uiState.value.boleto != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(boletoOpen = true, boletoLoading = true, boletoError = null)
            }
            getBoleto(invoice).fold(
                onSuccess = { content ->
                    _uiState.update {
                        it.copy(boleto = content, boletoLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(boletoLoading = false, boletoError = error)
                    }
                },
            )
        }
    }

    fun closeBoleto() = _uiState.update {
        it.copy(
            boletoOpen = false,
            boleto = null,
            boletoError = null,
            boletoLoading = false,
        )
    }

    fun sendCopyByEmail() = send(SendChannel.EMAIL)

    private fun send(channel: SendChannel) {
        val id = invoiceId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(sendingCopy = true, actionError = null) }
            sendCopy(id, channel).fold(
                onSuccess = { message ->
                    _uiState.update { it.copy(sendingCopy = false, sentCopyMessage = message) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(sendingCopy = false, actionError = error) }
                },
            )
        }
    }

    fun requestPromise() {
        viewModelScope.launch {
            _uiState.update { it.copy(requestingPromise = true, actionError = null) }
            requestPaymentPromise().fold(
                onSuccess = { promise ->
                    _uiState.update { it.copy(requestingPromise = false, promise = promise) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(requestingPromise = false, actionError = error) }
                },
            )
        }
    }

    fun consumeActionError() = _uiState.update { it.copy(actionError = null) }
    fun consumeSentCopyMessage() = _uiState.update { it.copy(sentCopyMessage = null) }
    fun consumePromise() = _uiState.update { it.copy(promise = null) }
}
