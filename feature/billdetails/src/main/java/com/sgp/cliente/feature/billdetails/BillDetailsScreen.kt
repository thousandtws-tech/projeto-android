package com.sgp.cliente.feature.billdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.common.format.MoneyFormatter
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpCopyableValue
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.dashedBorder
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonBillDetails
import com.sgp.cliente.core.designsystem.components.SgpSecondaryButton
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.InvoiceStatus
import java.time.LocalDate

/**
 * Invoice details in a ModalBottomSheet — keeps the list context behind it
 * and dismisses with a downward swipe or tapping the scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsSheet(
    invoiceId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillDetailsViewModel = hiltViewModel(),
) {
    LaunchedEffect(invoiceId) { viewModel.show(invoiceId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbar.showSnackbar(it.toUserMessage())
            viewModel.consumeActionError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            when {
                uiState.loading -> SgpSkeletonBillDetails(Modifier.fillMaxSize())
                uiState.error != null ->
                    SgpErrorView(uiState.error!!, viewModel::load, Modifier.fillMaxSize())
                else -> DetailsContent(uiState, viewModel)
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.md),
            )
        }
    }

    uiState.sentCopyMessage?.let { message ->
        InfoDialog("Segunda via", message, viewModel::consumeSentCopyMessage)
    }

    uiState.promise?.let { promise ->
        InfoDialog(
            title = if (promise.released) "Promessa registrada" else "Promessa não liberada",
            text = buildString {
                if (promise.message.isNotBlank()) append(promise.message)
                promise.protocol?.let {
                    if (isNotEmpty()) append("\n")
                    append("Protocolo: $it")
                }
            }.ifBlank { "Operação concluída." },
            onDismiss = viewModel::consumePromise,
        )
    }
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun DetailsContent(uiState: BillDetailsUiState, viewModel: BillDetailsViewModel) {
    val invoice = uiState.invoice ?: return
    val overdue = invoice.isOverdue(LocalDate.now())
    val isOpen = invoice.status == InvoiceStatus.OPEN

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            SgpCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
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
                    Text(
                        text = MoneyFormatter.brl(invoice.displayAmount),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Vencimento: ${DateFormatter.display(invoice.effectiveOrOriginalDueDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    invoice.documentNumber?.let {
                        Text(
                            text = "Documento nº $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        invoice.barcode?.let { barcode ->
            item {
                SgpCard {
                    SgpCopyableValue(
                        label = "Linha digitável",
                        value = barcode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        uiState.pixCode?.let { pix ->
            item {
                SgpCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text = "Pagar com PIX",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Copie o código abaixo e use o Pix Copia e Cola " +
                                "no aplicativo do seu banco para fazer o pagamento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SgpCopyableValue(
                            label = "PIX copia e cola",
                            value = pix,
                            modifier = Modifier
                                .fillMaxWidth()
                                .dashedBorder(
                                    color = MaterialTheme.colorScheme.outline,
                                    strokeWidth = 1.5.dp,
                                    cornerRadius = 14.dp,
                                )
                                .padding(Spacing.sm),
                        )
                    }
                }
            }
        }

        if (isOpen && uiState.canRequestPix) {
            item {
                SgpSecondaryButton(
                    text = "Gerar código PIX",
                    onClick = viewModel::loadPix,
                )
            }
        }

        if (isOpen) {
            item {
                SgpSecondaryButton(
                    text = "Receber 2ª via por e-mail",
                    onClick = viewModel::sendCopyByEmail,
                    enabled = !uiState.sendingCopy,
                )
            }
            item {
                SgpPrimaryButton(
                    text = "Solicitar promessa de pagamento",
                    onClick = viewModel::requestPromise,
                    loading = uiState.requestingPromise,
                )
            }
        }
    }
}
