package com.sgp.cliente.feature.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.usecase.invoice.GetBoletoUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 25

data class BillsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val invoices: List<Invoice> = emptyList(),
    val nextOffset: Int = 0,
    val endReached: Boolean = false,
    val error: AppError? = null,
    /** Invoice whose boleto PDF is shown in the in-app viewer. */
    val boletoInvoice: Invoice? = null,
    /** Downloaded boleto content — PDF pages or a bank web page. */
    val boleto: BoletoContent? = null,
    val boletoLoading: Boolean = false,
    val boletoError: AppError? = null,
) {
    /** Competence label (e.g. `janeiro/2026`) → invoices, newest group first. */
    val grouped: List<Pair<String, List<Invoice>>> by lazy {
        invoices
            .groupBy { it.dueDate?.let(DateFormatter::competenceLabel) ?: "Sem vencimento" }
            .map { (label, list) ->
                label to list.sortedByDescending { it.dueDate }
            }
            .sortedByDescending { (_, list) -> list.firstOrNull()?.dueDate }
    }
}

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val getInvoices: GetInvoicesUseCase,
    private val getBoleto: GetBoletoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            fetch(offset = 0, replace = true, forceRefresh = true)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || state.endReached) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            fetch(offset = state.nextOffset, replace = false, forceRefresh = false)
        }
    }

    private suspend fun fetch(offset: Int, replace: Boolean, forceRefresh: Boolean) {
        var next = offset
        var first = true
        var keepGoing = true
        while (keepGoing) {
            keepGoing = false
            getInvoices(
                statusFilter = STATUS_FILTER,
                offset = next,
                limit = PAGE_SIZE,
                forceRefresh = forceRefresh && first,
            ).fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        val merged = if (replace && first) {
                            page.items
                        } else {
                            (state.invoices + page.items).distinctBy(Invoice::id)
                        }
                        state.copy(
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            invoices = merged,
                            nextOffset = page.nextOffset,
                            endReached = !page.hasMore,
                            error = null,
                        )
                    }
                    // A filtered page can come back empty while the raw
                    // stream still has more — keep pulling until a match
                    // shows or the stream ends.
                    if (page.hasMore && _uiState.value.invoices.isEmpty()) {
                        keepGoing = true
                        next = page.nextOffset
                        first = false
                    }
                },
                onFailure = { error ->
                    AppLog.w(TAG, "invoice fetch failed offset=$next: ${error::class.simpleName}")
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            error = error,
                        )
                    }
                },
            )
        }
    }

    /**
     * "Abrir boleto" on a card: fetch the pre-signed boleto content
     * through the API client and hand the bytes to the in-app viewer.
     * Re-tapping the same invoice while it is open is a no-op.
     */
    fun openBoleto(invoice: Invoice) {
        if (invoice.boletoPath == null) return
        val state = _uiState.value
        if (state.boletoInvoice?.id == invoice.id && (state.boletoLoading || state.boleto != null)) {
            return
        }
        _uiState.update {
            it.copy(
                boletoInvoice = invoice,
                boleto = null,
                boletoError = null,
                boletoLoading = true,
            )
        }
        viewModelScope.launch {
            getBoleto(invoice).fold(
                onSuccess = { content ->
                    _uiState.update { it.copy(boleto = content, boletoLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(boletoError = error, boletoLoading = false) }
                },
            )
        }
    }

    fun closeBoleto() = _uiState.update {
        it.copy(boletoInvoice = null, boleto = null, boletoError = null, boletoLoading = false)
    }

    private companion object {
        const val TAG = "feature.bills"

        /** The screen lists paid bills only — no status picker (client ask). */
        val STATUS_FILTER = InvoiceStatus.PAID
    }
}
