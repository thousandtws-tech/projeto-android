package com.sgp.cliente.feature.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.format.MoneyFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceCard
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceList
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    onOpenInvoice: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Faturas") }) },
        modifier = modifier,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusFilters(uiState.filter, viewModel::onFilterChange)
            PullToRefreshBox(
                // Hide the spinner while the skeleton is showing — they
                // represent the same load, showing both is visual noise.
                isRefreshing = uiState.refreshing && !uiState.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                SgpContentFrame(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.loading -> SgpSkeletonInvoiceList(Modifier.fillMaxSize())
                        uiState.error != null && uiState.invoices.isEmpty() ->
                            SgpErrorView(uiState.error!!, viewModel::refresh, Modifier.fillMaxSize())
                        uiState.invoices.isEmpty() ->
                            SgpEmptyState(
                                title = "Nenhuma fatura",
                                message = "Não há faturas para o filtro selecionado.",
                                modifier = Modifier.fillMaxSize(),
                            )
                        else -> InvoiceList(uiState, listState, onOpenInvoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilters(selected: InvoiceStatus?, onSelect: (InvoiceStatus?) -> Unit) {
    val options = listOf(
        null to "Todas",
        InvoiceStatus.OPEN to "Em aberto",
        InvoiceStatus.PAID to "Pagas",
        InvoiceStatus.CANCELLED to "Canceladas",
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(options.size) { index ->
            val (status, label) = options[index]
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun InvoiceList(
    uiState: BillsUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenInvoice: (Int) -> Unit,
) {
    val today = remember { LocalDate.now() }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        uiState.grouped.forEach { (competence, invoices) ->
            item(key = "header_$competence") {
                Text(
                    text = competence,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
            items(invoices, key = { it.id }) { invoice ->
                InvoiceRow(
                    invoice = invoice,
                    today = today,
                    onClick = { onOpenInvoice(invoice.id) },
                )
            }
        }
        if (uiState.loadingMore) {
            item(key = "loading_more") {
                SgpSkeletonInvoiceCard()
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, today: LocalDate, onClick: () -> Unit) {
    val overdue = invoice.isOverdue(today)
    SgpCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = MoneyFormatter.brl(invoice.displayAmount),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Vencimento ${DateFormatter.display(invoice.effectiveOrOriginalDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SgpStatusBadge(
                text = when {
                    overdue -> "Vencida"
                    invoice.status == InvoiceStatus.PAID -> "Paga"
                    invoice.status == InvoiceStatus.CANCELLED -> "Cancelada"
                    else -> "Em aberto"
                },
                kind = when {
                    overdue -> SgpBadgeKind.OVERDUE
                    invoice.status == InvoiceStatus.PAID -> SgpBadgeKind.PAID
                    invoice.status == InvoiceStatus.CANCELLED -> SgpBadgeKind.NEUTRAL
                    else -> SgpBadgeKind.OPEN
                },
            )
        }
    }
}
