package com.sgp.cliente.feature.paymentpromises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.format.MoneyFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonRows
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPromisesScreen(
    modifier: Modifier = Modifier,
    viewModel: PaymentPromisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbar.showSnackbar(it.toUserMessage())
            viewModel.consumeActionError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Promessa de pagamento") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier,
    ) { padding ->
        SgpContentFrame(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.loading -> SgpSkeletonRows(Modifier.fillMaxSize())
                uiState.error != null ->
                    SgpErrorView(uiState.error!!, viewModel::load, Modifier.fillMaxSize())
                uiState.openInvoices.all.isEmpty() ->
                    SgpEmptyState(
                        title = "Nenhuma fatura em aberto",
                        message = "Promessas de pagamento só fazem sentido com faturas em aberto.",
                        modifier = Modifier.fillMaxSize(),
                    )
                else -> PromiseContent(uiState, viewModel)
            }
        }
    }

    uiState.result?.let { promise ->
        AlertDialog(
            onDismissRequest = viewModel::consumeResult,
            title = { Text(if (promise.released) "Promessa registrada" else "Promessa não liberada") },
            text = {
                Text(
                    buildString {
                        if (promise.message.isNotBlank()) append(promise.message)
                        promise.protocol?.let {
                            if (isNotEmpty()) append("\n")
                            append("Protocolo: $it")
                        }
                    }.ifBlank { "Operação concluída." },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeResult) { Text("OK") }
            },
        )
    }
}

@Composable
private fun PromiseContent(uiState: PaymentPromisesUiState, viewModel: PaymentPromisesViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            Text(
                text = "A promessa de pagamento é registrada no contrato e pode liberar " +
                    "temporariamente o acesso, conforme as regras do provedor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(uiState.openInvoices.all, key = { it.id }) { invoice ->
            SgpCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = MoneyFormatter.brl(invoice.displayAmount),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Vence ${DateFormatter.display(invoice.effectiveOrOriginalDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SgpPrimaryButton(
                text = "Solicitar promessa de pagamento",
                onClick = viewModel::request,
                loading = uiState.requesting,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
