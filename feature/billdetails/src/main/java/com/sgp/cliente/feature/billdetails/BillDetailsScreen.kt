package com.sgp.cliente.feature.billdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sgp.cliente.core.designsystem.components.BoletoViewerDialog
import com.sgp.cliente.core.designsystem.components.SgpBadgeKind
import com.sgp.cliente.core.designsystem.components.SgpCard
import com.sgp.cliente.core.designsystem.components.SgpCopyableValue
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonBillDetails
import com.sgp.cliente.core.designsystem.components.SgpSecondaryButton
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.BoletoContent
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
        Box(Modifier.fillMaxWidth()) {
            when {
                uiState.loading -> SgpSkeletonBillDetails(
                    Modifier.fillMaxWidth().height(420.dp),
                )
                uiState.error != null ->
                    SgpErrorView(
                        uiState.error!!,
                        viewModel::load,
                        Modifier.fillMaxWidth().height(420.dp),
                    )
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

    if (uiState.boletoOpen) {
        val invoice = uiState.invoice
        BoletoViewerDialog(
            title = invoice?.documentNumber?.let { "Boleto · Nº $it" } ?: "Boleto",
            fileKey = invoice?.id?.toString() ?: "invoice",
            pdf = (uiState.boleto as? BoletoContent.Pdf)?.bytes,
            webUrl = (uiState.boleto as? BoletoContent.ExternalPage)?.url,
            loading = uiState.boletoLoading,
            error = uiState.boletoError,
            onRetry = viewModel::openBoleto,
            onDismiss = viewModel::closeBoleto,
        )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
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

        invoice.pixCode?.let { pix ->
            SgpCard {
                SgpCopyableValue(
                    label = "PIX copia e cola",
                    value = pix,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isOpen && invoice.pixCode == null && invoice.canGeneratePix) {
            SgpPrimaryButton(
                text = "Gerar código PIX",
                onClick = viewModel::generatePix,
                loading = uiState.generatingPix,
            )
        }

        invoice.barcode?.let { barcode ->
            SgpCard {
                SgpCopyableValue(
                    label = "Linha digitável",
                    value = barcode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isOpen && invoice.boletoPath != null) {
            SgpPrimaryButton(
                text = "Abrir boleto",
                onClick = viewModel::openBoleto,
                loading = uiState.boletoLoading && uiState.boletoOpen,
            )
        }

        if (isOpen) {
            SgpSecondaryButton(
                text = "Receber 2ª via por e-mail",
                onClick = viewModel::sendCopyByEmail,
                enabled = !uiState.sendingCopy,
            )
            SgpPrimaryButton(
                text = "Solicitar promessa de pagamento",
                onClick = viewModel::requestPromise,
                loading = uiState.requestingPromise,
            )
        }
    }
}
