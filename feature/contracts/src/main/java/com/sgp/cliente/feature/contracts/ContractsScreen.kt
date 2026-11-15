package com.sgp.cliente.feature.contracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceList
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.Contract

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsScreen(
    onContractSelected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContractsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ContractsEvent.ContractSelected -> onContractSelected()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Selecione o contrato") }) },
        modifier = modifier,
    ) { padding ->
        SgpContentFrame(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val visible = uiState.visibleContracts
            when {
                uiState.loading || (uiState.probing && uiState.openBillCounts.isEmpty()) ->
                    SgpSkeletonInvoiceList(Modifier.fillMaxSize())

                visible.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SgpEmptyState(
                        title = "Nenhum contrato com boleto em aberto",
                        message = "Só aparecem aqui contratos com boleto gerado ou em aberto.",
                        modifier = Modifier.padding(Spacing.lg),
                    )
                    TextButton(onClick = viewModel::toggleShowAll) {
                        Text("Ver todos os contratos")
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(visible, key = { it.id }) { contract ->
                        ContractCard(
                            contract = contract,
                            openBills = uiState.openBillCounts[contract.id],
                            selected = contract.id == uiState.selectedContractId,
                            onClick = { viewModel.select(contract.id) },
                        )
                    }
                    if (uiState.hiddenCount > 0) {
                        item {
                            TextButton(
                                onClick = viewModel::toggleShowAll,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (uiState.showAll) {
                                        "Ocultar contratos sem boleto"
                                    } else {
                                        "Ver todos os contratos (${uiState.hiddenCount} ocultos)"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContractCard(
    contract: Contract,
    openBills: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SgpCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contrato ${contract.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                contract.internetPlan?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Vencimento dia ${contract.dueDay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (openBills != null && openBills > 0) {
                    Text(
                        text = if (openBills == 1) "1 boleto em aberto" else "$openBills boletos em aberto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                SgpStatusBadge(
                    text = contract.status.trim(),
                    kind = if (contract.isActive) SgpBadgeKind.OPEN else SgpBadgeKind.NEUTRAL,
                )
            }
        }
    }
}
