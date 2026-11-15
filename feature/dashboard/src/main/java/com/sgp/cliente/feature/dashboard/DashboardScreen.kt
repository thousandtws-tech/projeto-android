package com.sgp.cliente.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sgp.cliente.core.designsystem.components.SgpSkeletonDashboard
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.SgpTextButton
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.Invoice
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
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

        if (uiState.openInvoices.overdue.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${uiState.openInvoices.overdue.size} fatura(s) vencida(s) em aberto",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
            }
        }

        item {
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

        if (uiState.all.isEmpty()) {
            item {
                SgpEmptyState(
                    title = "Nenhuma fatura em aberto",
                    message = "Você não possui boletos pendentes no momento.",
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
                )
            }
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
