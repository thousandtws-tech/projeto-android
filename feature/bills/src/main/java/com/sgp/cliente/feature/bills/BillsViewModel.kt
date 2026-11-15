package com.sgp.cliente.feature.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
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
    val filter: InvoiceStatus? = null,
    val nextOffset: Int = 0,
    val endReached: Boolean = false,
    val error: AppError? = null,
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

    private companion object {
        const val TAG = "feature.bills"
    }
}
