package com.sgp.cliente.feature.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.format.MoneyFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.adaptive.currentWindowWidthSizeClass
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.BoletoViewerDialog
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceCard
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceList
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.theme.LayoutTokens
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.BoletoContent
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
    val gridState = rememberLazyGridState()
    val snackbar = remember { SnackbarHostState() }
    val onOpenBoleto: (Invoice) -> Unit = viewModel::openBoleto

    val shouldLoadMore by remember {
        derivedStateOf {
            // Only one container is composed at a time (list on Compact,
            // grid otherwise) — the idle one reports zero items, so
            // checking both is safe.
            val listInfo = listState.layoutInfo
            val gridInfo = gridState.layoutInfo
            val totalItems = listInfo.totalItemsCount + gridInfo.totalItemsCount
            val lastVisible = maxOf(
                listInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                gridInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
            )
            totalItems > 0 && lastVisible >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                                title = "Nenhuma fatura paga",
                                message = "Não há faturas pagas para exibir.",
                                modifier = Modifier.fillMaxSize(),
                            )
                        else -> InvoiceList(
                            uiState,
                            listState,
                            gridState,
                            onOpenInvoice,
                            onOpenBoleto,
                        )
                    }
                }
            }
        }
    }

    // In-app boleto viewer — downloads through the API client and renders
    // with the platform PdfRenderer instead of an external browser.
    uiState.boletoInvoice?.let { invoice ->
        BoletoViewerDialog(
            title = invoice.documentNumber?.let { "Boleto · Nº $it" } ?: "Boleto",
            fileKey = invoice.id.toString(),
            pdf = (uiState.boleto as? BoletoContent.Pdf)?.bytes,
            webUrl = (uiState.boleto as? BoletoContent.ExternalPage)?.url,
            loading = uiState.boletoLoading,
            error = uiState.boletoError,
            onRetry = { viewModel.openBoleto(invoice) },
            onDismiss = viewModel::closeBoleto,
        )
    }
}

@Composable
private fun InvoiceList(
    uiState: BillsUiState,
    listState: LazyListState,
    gridState: LazyGridState,
    onOpenInvoice: (Int) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
) {
    val today = remember { LocalDate.now() }
    if (currentWindowWidthSizeClass() == WindowWidthSizeClass.Compact) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            billItems(uiState, today, onOpenInvoice, onOpenBoleto)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(LayoutTokens.gridItemMinWidth),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            billItems(uiState, today, onOpenInvoice, onOpenBoleto)
        }
    }
}

private fun LazyListScope.billItems(
    uiState: BillsUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
) {
    uiState.grouped.forEach { (competence, invoices) ->
        item(key = "header_$competence") {
            CompetenceHeader(competence, invoices)
        }
        items(invoices, key = { it.id }) { invoice ->
            InvoiceCard(
                invoice = invoice,
                today = today,
                onOpenInvoice = { onOpenInvoice(invoice.id) },
                onOpenBoleto = onOpenBoleto,
            )
        }
    }
    if (uiState.loadingMore) {
        item(key = "loading_more") {
            SgpSkeletonInvoiceCard()
        }
    } else if (uiState.endReached && uiState.invoices.size > 5) {
        item(key = "end_of_list") {
            EndOfHistory()
        }
    }
}

private fun LazyGridScope.billItems(
    uiState: BillsUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
) {
    uiState.grouped.forEach { (competence, invoices) ->
        item(key = "header_$competence", span = { GridItemSpan(maxLineSpan) }) {
            CompetenceHeader(competence, invoices)
        }
        items(invoices, key = { it.id }) { invoice ->
            InvoiceCard(
                invoice = invoice,
                today = today,
                onOpenInvoice = { onOpenInvoice(invoice.id) },
                onOpenBoleto = onOpenBoleto,
            )
        }
    }
    if (uiState.loadingMore) {
        item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
            SgpSkeletonInvoiceCard()
        }
    } else if (uiState.endReached && uiState.invoices.size > 5) {
        item(key = "end_of_list", span = { GridItemSpan(maxLineSpan) }) {
            EndOfHistory()
        }
    }
}

/** Group label + real count/subtotal so the user sees the month's weight. */
@Composable
private fun CompetenceHeader(competence: String, invoices: List<Invoice>) {
    val subtotal = invoices.sumOf { it.displayAmount ?: 0.0 }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = competence.replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${invoices.size} " +
                (if (invoices.size == 1) "fatura" else "faturas") +
                " · ${MoneyFormatter.brl(subtotal)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Full invoice card in the reference style: status badge + "Ver fatura",
 * due/amount/document info, and inline payment actions for open invoices
 * (copy barcode, copy PIX, open boleto) so paying needs no extra screens. */
@Composable
private fun InvoiceCard(
    invoice: Invoice,
    today: LocalDate,
    onOpenInvoice: () -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
) {
    val overdue = invoice.isOverdue(today)
    val payable = invoice.status == InvoiceStatus.OPEN

    Surface(
        onClick = onOpenInvoice,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SgpStatusBadge(
                    text = when {
                        overdue -> "Vencida · Prioridade"
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
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onOpenInvoice,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text("Ver fatura", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                InvoiceInfo(
                    label = "Vencimento",
                    value = DateFormatter.display(invoice.effectiveOrOriginalDueDate),
                    modifier = Modifier.weight(1f),
                )
                InvoiceInfo(
                    label = "Valor",
                    value = MoneyFormatter.brl(invoice.displayAmount),
                    modifier = Modifier.weight(1f),
                )
            }

            invoice.documentNumber?.let { doc ->
                InvoiceInfo(label = "Nº da fatura", value = doc.toString())
            }

            // Single payment CTA per the client's ask: just the boleto.
            if (payable && invoice.boletoPath != null) {
                HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.12f))
                SgpPrimaryButton(
                    text = "Abrir boleto",
                    onClick = { onOpenBoleto(invoice) },
                )
            }
        }
    }
}

@Composable
private fun InvoiceInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EndOfHistory() {
    Text(
        text = "Você viu todas as faturas pagas",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
    )
}

private fun previewInvoice(
    id: Int,
    status: InvoiceStatus,
    dueDate: LocalDate,
) = Invoice(
    id = id,
    documentNumber = 100000L + id,
    statusId = status.apiId,
    statusLabel = "",
    dueDate = dueDate,
    effectiveDueDate = null,
    amount = 99.90 + id,
    correctedAmount = null,
    paidAt = if (status == InvoiceStatus.PAID) dueDate else null,
    barcode = null,
    pixCode = null,
    canGeneratePix = false,
    boletoPath = null,
)

private val previewUiState = BillsUiState(
    loading = false,
    invoices = listOf(
        previewInvoice(1, InvoiceStatus.PAID, LocalDate.now()),
        previewInvoice(2, InvoiceStatus.PAID, LocalDate.now().minusMonths(1)),
        previewInvoice(3, InvoiceStatus.PAID, LocalDate.now().minusMonths(1).minusDays(3)),
        previewInvoice(4, InvoiceStatus.PAID, LocalDate.now().minusMonths(2)),
    ),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun InvoiceListCompactPreview() {
    SgpTheme {
        InvoiceList(
            uiState = previewUiState,
            listState = rememberLazyListState(),
            gridState = rememberLazyGridState(),
            onOpenInvoice = {},
            onOpenBoleto = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700)
@Composable
private fun InvoiceListGridPreview() {
    SgpTheme {
        InvoiceList(
            uiState = previewUiState,
            listState = rememberLazyListState(),
            gridState = rememberLazyGridState(),
            onOpenInvoice = {},
            onOpenBoleto = {},
        )
    }
}
