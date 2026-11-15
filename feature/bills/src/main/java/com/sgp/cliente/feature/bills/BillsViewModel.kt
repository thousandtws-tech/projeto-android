package com.sgp.cliente.feature.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.usecase.invoice.GetBoletoPdfUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoicesUseCase
import com.sgp.cliente.domain.usecase.payment.GeneratePixUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val PAGE_SIZE = 25

data class BillsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val invoices: List<Invoice> = emptyList(),
    val filter: InvoiceStatus? = null,
    val nextOffset: Int = 0,
    val endReached: Boolean = false,
    val error: AppError? = null,
    /** Invoice currently generating a PIX code (per-card spinner). */
    val generatingPixId: Int? = null,
    /** One-shot: a PIX code ready to be copied by the UI. */
    val pixCodeToCopy: String? = null,
    /** One-shot action error surfaced as a snackbar. */
    val actionError: AppError? = null,
    /** Invoice whose boleto PDF is shown in the in-app viewer. */
    val boletoInvoice: Invoice? = null,
    /** Downloaded boleto PDF bytes (rendered by the viewer). */
    val boletoPdf: ByteArray? = null,
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

    /** Open-invoice snapshot for the summary card: count, real subtotal and
     * the nearest effective due date (upcoming first, overdue otherwise). */
    fun openSummary(today: LocalDate): OpenSummary {
        val open = invoices.filter { it.status == InvoiceStatus.OPEN }
        val dues = open.mapNotNull { it.effectiveOrOriginalDueDate }
        val nextDue = dues.filter { !it.isBefore(today) }.minOrNull()
            ?: dues.maxOrNull()
        return OpenSummary(
            count = open.size,
            total = open.sumOf { it.displayAmount ?: 0.0 },
            nextDue = nextDue,
        )
    }
}

data class OpenSummary(
    val count: Int,
    val total: Double,
    val nextDue: LocalDate?,
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val getInvoices: GetInvoicesUseCase,
    private val generatePix: GeneratePixUseCase,
    private val getBoletoPdf: GetBoletoPdfUseCase,
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

    fun onFilterChange(filter: InvoiceStatus?) {
        if (filter == _uiState.value.filter) return
        _uiState.update {
            it.copy(filter = filter, loading = true, invoices = emptyList(), nextOffset = 0)
        }
        viewModelScope.launch {
            fetch(offset = 0, replace = true, forceRefresh = false)
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
        getInvoices(
            statusFilter = _uiState.value.filter,
            offset = offset,
            limit = PAGE_SIZE,
            forceRefresh = forceRefresh,
        ).fold(
            onSuccess = { page ->
                _uiState.update { state ->
                    val merged = if (replace) page.items
                    else (state.invoices + page.items).distinctBy(Invoice::id)
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
            },
            onFailure = { error ->
                AppLog.w(TAG, "invoice fetch failed offset=$offset: ${error::class.simpleName}")
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

    /**
     * "Copiar PIX" on a card: copies the inline code when the API already
     * delivered one, otherwise generates it on demand (`titulo/{id}/pix/`)
     * and hands the code to the UI for clipboard + keeps it on the invoice
     * so the next tap copies instantly.
     */
    fun requestPixCode(invoice: Invoice) {
        val inline = invoice.pixCode
        if (inline != null) {
            _uiState.update { it.copy(pixCodeToCopy = inline) }
            return
        }
        if (!invoice.canGeneratePix || _uiState.value.generatingPixId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(generatingPixId = invoice.id, actionError = null) }
            generatePix(invoice.id).fold(
                onSuccess = { pix ->
                    val code = pix.code
                    _uiState.update { state ->
                        state.copy(
                            generatingPixId = null,
                            pixCodeToCopy = if (pix.isSuccess) code else null,
                            actionError = if (pix.isSuccess) {
                                null
                            } else {
                                AppError.Api(pix.message)
                            },
                            invoices = if (code != null) {
                                state.invoices.map {
                                    if (it.id == invoice.id) it.copy(pixCode = code) else it
                                }
                            } else {
                                state.invoices
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(generatingPixId = null, actionError = error)
                    }
                },
            )
        }
    }

    /**
     * "Abrir boleto" / PIX-in-boleto on a card: fetch the pre-signed PDF
     * through the API client and hand the bytes to the in-app viewer.
     * Re-tapping the same invoice while it is open is a no-op.
     */
    fun openBoleto(invoice: Invoice) {
        if (invoice.boletoPath == null) return
        val state = _uiState.value
        if (state.boletoInvoice?.id == invoice.id && (state.boletoLoading || state.boletoPdf != null)) {
            return
        }
        _uiState.update {
            it.copy(boletoInvoice = invoice, boletoPdf = null, boletoError = null, boletoLoading = true)
        }
        viewModelScope.launch {
            getBoletoPdf(invoice).fold(
                onSuccess = { bytes ->
                    _uiState.update { it.copy(boletoPdf = bytes, boletoLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(boletoError = error, boletoLoading = false) }
                },
            )
        }
    }

    fun closeBoleto() = _uiState.update {
        it.copy(boletoInvoice = null, boletoPdf = null, boletoError = null, boletoLoading = false)
    }

    fun consumePixCodeToCopy() = _uiState.update { it.copy(pixCodeToCopy = null) }
    fun consumeActionError() = _uiState.update { it.copy(actionError = null) }

    private companion object {
        const val TAG = "feature.bills"
    }
}
