package com.sgp.cliente.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.format.MoneyFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.adaptive.currentWindowWidthSizeClass
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpSkeletonDashboard
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.SgpTextButton
import com.sgp.cliente.core.designsystem.theme.LayoutTokens
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Início") }) },
        modifier = modifier,
    ) { padding ->
        PullToRefreshBox(
            // Hide the spinner while the skeleton is showing — they
            // represent the same load, showing both is visual noise.
            isRefreshing = uiState.refreshing && !uiState.loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            SgpContentFrame(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.loading -> SgpSkeletonDashboard(Modifier.fillMaxSize())
                    uiState.error != null && uiState.all.isEmpty() ->
                        SgpErrorView(uiState.error!!, viewModel::refresh, Modifier.fillMaxSize())
                    else -> DashboardContent(uiState, onOpenInvoice, onOpenBills)
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
) {
    val today = LocalDate.now()
    if (currentWindowWidthSizeClass() == WindowWidthSizeClass.Compact) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            dashboardItems(uiState, today, onOpenInvoice, onOpenBills)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(LayoutTokens.gridItemMinWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            dashboardItems(uiState, today, onOpenInvoice, onOpenBills)
        }
    }
}

private fun LazyListScope.dashboardItems(
    uiState: DashboardUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
) {
    item { ContractHeader(uiState) }

    if (uiState.openInvoices.overdue.isNotEmpty()) {
        item { OverdueBanner(uiState.openInvoices.overdue.size) }
    }

    item { OpenInvoicesHeader(onOpenBills) }

    if (uiState.all.isEmpty()) {
        item { OpenInvoicesEmpty() }
    } else {
        items(uiState.all, key = { it.id }) { invoice ->
            InvoiceSummaryCard(
                invoice = invoice,
                today = today,
                onClick = { onOpenInvoice(invoice.id) },
            )
        }
    }
}

private fun LazyGridScope.dashboardItems(
    uiState: DashboardUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
) {
    // Non-card elements (contract summary, banner, section header, empty
    // state) stretch across all columns; invoice cards fill grid cells.
    item(span = { GridItemSpan(maxLineSpan) }) { ContractHeader(uiState) }

    if (uiState.openInvoices.overdue.isNotEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OverdueBanner(uiState.openInvoices.overdue.size)
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }) { OpenInvoicesHeader(onOpenBills) }

    if (uiState.all.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) { OpenInvoicesEmpty() }
    } else {
        items(uiState.all, key = { it.id }) { invoice ->
            InvoiceSummaryCard(
                invoice = invoice,
                today = today,
                onClick = { onOpenInvoice(invoice.id) },
            )
        }
    }
}

@Composable
private fun ContractHeader(uiState: DashboardUiState) {
    uiState.contract?.let { contract ->
        SgpCard {
            Text(
                text = uiState.session?.customerName ?: contract.razaoSocial,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Contrato ${contract.id}" +
                    (contract.internetPlan?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverdueBanner(overdueCount: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$overdueCount fatura(s) vencida(s) em aberto",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

@Composable
private fun OpenInvoicesHeader(onOpenBills: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Faturas em aberto",
            style = MaterialTheme.typography.titleSmall,
        )
        SgpTextButton(text = "Ver todas", onClick = onOpenBills)
    }
}

@Composable
private fun OpenInvoicesEmpty() {
    SgpEmptyState(
        title = "Nenhuma fatura em aberto",
        message = "Você não possui boletos pendentes no momento.",
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
    )
}

@Composable
private fun InvoiceSummaryCard(invoice: Invoice, today: LocalDate, onClick: () -> Unit) {
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
                    text = "Vence em ${DateFormatter.display(invoice.effectiveOrOriginalDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SgpStatusBadge(
                text = if (overdue) "Vencida" else "Em aberto",
                kind = if (overdue) SgpBadgeKind.OVERDUE else SgpBadgeKind.OPEN,
            )
        }
    }
}

private fun previewInvoice(id: Int, dueDate: LocalDate) = Invoice(
    id = id,
    documentNumber = 200000L + id,
    statusId = InvoiceStatus.OPEN.apiId,
    statusLabel = "",
    dueDate = dueDate,
    effectiveDueDate = null,
    amount = 99.90 + id,
    correctedAmount = null,
    paidAt = null,
    barcode = null,
    pixCode = null,
    canGeneratePix = false,
    boletoPath = null,
)

private val previewUiState = DashboardUiState(
    loading = false,
    session = CustomerSession(
        credentials = Credentials(login = "maria@exemplo.com", password = ""),
        customerName = "Maria Souza",
        contracts = listOf(
            Contract(
                id = 1001,
                status = "Ativo",
                razaoSocial = "Maria Souza",
                cpfCnpj = "12345678900",
                dueDay = 10,
                internetPlan = "Fibra 300 Mega",
                internetPlanValue = 99.90,
                internetPlanId = null,
            ),
        ),
        selectedContractId = 1001,
    ),
    openInvoices = OpenInvoices(
        overdue = listOf(previewInvoice(1, LocalDate.now().minusDays(3))),
        upcoming = listOf(
            previewInvoice(2, LocalDate.now().plusDays(7)),
            previewInvoice(3, LocalDate.now().plusDays(20)),
        ),
    ),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun DashboardContentCompactPreview() {
    SgpTheme {
        DashboardContent(
            uiState = previewUiState,
            onOpenInvoice = {},
            onOpenBills = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 700)
@Composable
private fun DashboardContentGridPreview() {
    SgpTheme {
        DashboardContent(
            uiState = previewUiState,
            onOpenInvoice = {},
            onOpenBills = {},
        )
    }
}
