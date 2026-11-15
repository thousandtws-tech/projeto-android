package com.sgp.cliente.feature.bills

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceCard
import com.sgp.cliente.core.designsystem.components.SgpSkeletonInvoiceList
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.LayoutTokens
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.core.designsystem.theme.StatusCancelled
import com.sgp.cliente.core.designsystem.theme.StatusOpen
import com.sgp.cliente.core.designsystem.theme.StatusPaid
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
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onCopyValue: (String) -> Unit = {
        clipboard.setText(AnnotatedString(it))
        scope.launch { snackbar.showSnackbar("Código copiado") }
    }
    val onOpenBoleto: (Invoice) -> Unit = { invoice ->
        invoice.boletoPath?.let { runCatching { uriHandler.openUri(it) } }
    }

    // A generated PIX code arrives through state — copy it here, where the
    // clipboard lives, then consume the one-shot.
    LaunchedEffect(uiState.pixCodeToCopy) {
        uiState.pixCodeToCopy?.let {
            clipboard.setText(AnnotatedString(it))
            viewModel.consumePixCodeToCopy()
            snackbar.showSnackbar("Código PIX copiado")
        }
    }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbar.showSnackbar(it.toUserMessage())
            viewModel.consumeActionError()
        }
    }

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
        topBar = { TopAppBar(title = { Text("Faturas") }) },
        snackbarHost = { SnackbarHost(snackbar) },
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
                        else -> InvoiceList(
                            uiState,
                            listState,
                            gridState,
                            onOpenInvoice,
                            onCopyValue,
                            onOpenBoleto,
                            viewModel::requestPixCode,
                        )
                    }
                }
            }
        }
    }
}

private data class FilterOption(
    val status: InvoiceStatus?,
    val label: String,
    val dotColor: Color?,
)

/** Animated pill segmented control: tonal track + sliding white indicator,
 * semantic status dots matching the invoice row badges. */
@Composable
private fun StatusFilters(selected: InvoiceStatus?, onSelect: (InvoiceStatus?) -> Unit) {
    val options = listOf(
        FilterOption(null, "Todas", null),
        FilterOption(InvoiceStatus.OPEN, "Em aberto", StatusOpen),
        FilterOption(InvoiceStatus.PAID, "Pagas", StatusPaid),
        FilterOption(InvoiceStatus.CANCELLED, "Canceladas", StatusCancelled),
    )
    val selectedIndex = options.indexOfFirst { it.status == selected }.coerceAtLeast(0)

    // Measured segment positions (content-sized, so the control still
    // scrolls horizontally on narrow screens).
    val segmentBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val target = segmentBounds[selectedIndex] ?: (0f to 0f)
    val indicatorX by animateFloatAsState(
        targetValue = target.first,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segX",
    )
    val indicatorW by animateFloatAsState(
        targetValue = target.second,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segW",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
    ) {
        if (indicatorW > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.roundToInt(), 0) }
                    .width(with(LocalDensity.current) { indicatorW.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
            )
        }
        Row {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            segmentBounds[index] =
                                coordinates.positionInParent().x to
                                    coordinates.size.width.toFloat()
                        }
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(option.status) }
                        .padding(horizontal = 14.dp),
                ) {
                    option.dotColor?.let { dot ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dot),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceList(
    uiState: BillsUiState,
    listState: LazyListState,
    gridState: LazyGridState,
    onOpenInvoice: (Int) -> Unit,
    onCopyValue: (String) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
    onPix: (Invoice) -> Unit,
) {
    val today = remember { LocalDate.now() }
    if (currentWindowWidthSizeClass() == WindowWidthSizeClass.Compact) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            billItems(uiState, today, onOpenInvoice, onCopyValue, onOpenBoleto, onPix)
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
            billItems(uiState, today, onOpenInvoice, onCopyValue, onOpenBoleto, onPix)
        }
    }
}

/** Summary is only meaningful alongside unpaid invoices — hidden under the
 * Pagas/Canceladas filters, same rule as the attention card. */
private fun BillsUiState.showOpenSummary(today: LocalDate): Boolean =
    (filter == null || filter == InvoiceStatus.OPEN) && openSummary(today).count > 0

private fun LazyListScope.billItems(
    uiState: BillsUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onCopyValue: (String) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
    onPix: (Invoice) -> Unit,
) {
    if (uiState.showOpenSummary(today)) {
        item(key = "open_summary") {
            OpenSummaryCard(uiState.openSummary(today))
        }
    }
    uiState.attentionInvoice(today)?.let { overdue ->
        item(key = "overdue_hero") {
            OverdueInvoiceCard(overdue, today) { onOpenInvoice(overdue.id) }
        }
    }
    uiState.grouped.forEach { (competence, invoices) ->
        item(key = "header_$competence") {
            CompetenceHeader(competence, invoices)
        }
        items(invoices, key = { it.id }) { invoice ->
            InvoiceCard(
                invoice = invoice,
                today = today,
                generatingPix = uiState.generatingPixId == invoice.id,
                onOpenInvoice = { onOpenInvoice(invoice.id) },
                onCopyValue = onCopyValue,
                onOpenBoleto = onOpenBoleto,
                onPix = { onPix(invoice) },
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
    onCopyValue: (String) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
    onPix: (Invoice) -> Unit,
) {
    if (uiState.showOpenSummary(today)) {
        item(key = "open_summary", span = { GridItemSpan(maxLineSpan) }) {
            OpenSummaryCard(uiState.openSummary(today))
        }
    }
    uiState.attentionInvoice(today)?.let { overdue ->
        item(key = "overdue_hero", span = { GridItemSpan(maxLineSpan) }) {
            OverdueInvoiceCard(overdue, today) { onOpenInvoice(overdue.id) }
        }
    }
    uiState.grouped.forEach { (competence, invoices) ->
        item(key = "header_$competence", span = { GridItemSpan(maxLineSpan) }) {
            CompetenceHeader(competence, invoices)
        }
        items(invoices, key = { it.id }) { invoice ->
            InvoiceCard(
                invoice = invoice,
                today = today,
                generatingPix = uiState.generatingPixId == invoice.id,
                onOpenInvoice = { onOpenInvoice(invoice.id) },
                onCopyValue = onCopyValue,
                onOpenBoleto = onOpenBoleto,
                onPix = { onPix(invoice) },
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

/** First overdue invoice worth surfacing — hidden when the active filter
 * cannot contain it (Pagas/Canceladas). */
private fun BillsUiState.attentionInvoice(today: LocalDate): Invoice? =
    if (filter == null || filter == InvoiceStatus.OPEN) {
        invoices.firstOrNull { it.isOverdue(today) }
    } else {
        null
    }

/** Attention card pinned to the top when an invoice is overdue — the
 * money-owed call to action users must not miss. */
@Composable
private fun OverdueInvoiceCard(invoice: Invoice, today: LocalDate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Fatura em atraso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onClick,
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "Pagar agora",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                text = MoneyFormatter.brl(invoice.displayAmount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Text(
                text = DateFormatter.dueWithRelative(invoice.effectiveOrOriginalDueDate, today),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
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

/** Glanceable open-debt summary — count, subtotal and nearest due date. */
@Composable
private fun OpenSummaryCard(summary: OpenSummary) {
    SgpCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryMetric(
                label = "Faturas em aberto",
                value = summary.count.toString(),
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "Valor em aberto",
                value = MoneyFormatter.brl(summary.total),
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "Próx. vencimento",
                value = summary.nextDue?.let(DateFormatter::display) ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
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
    generatingPix: Boolean,
    onOpenInvoice: () -> Unit,
    onCopyValue: (String) -> Unit,
    onOpenBoleto: (Invoice) -> Unit,
    onPix: () -> Unit,
) {
    val overdue = invoice.isOverdue(today)
    val payable = invoice.status == InvoiceStatus.OPEN
    val containerColor = if (overdue) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (overdue) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onOpenInvoice,
        color = containerColor,
        contentColor = contentColor,
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
                OutlinedButton(
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

            if (payable) {
                HorizontalDivider(color = contentColor.copy(alpha = 0.12f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    PaymentAction(
                        icon = Icons.Outlined.ContentCopy,
                        title = "Copiar código",
                        caption = "Linha digitável",
                        enabled = invoice.barcode != null,
                        modifier = Modifier.weight(1f),
                    ) { onCopyValue(invoice.barcode!!) }
                    PaymentAction(
                        icon = Icons.Filled.QrCode2,
                        title = "Copiar PIX",
                        caption = when {
                            invoice.pixCode != null -> "Pague agora"
                            invoice.canGeneratePix -> "Gerar agora"
                            else -> "Indisponível"
                        },
                        enabled = invoice.pixCode != null || invoice.canGeneratePix,
                        loading = generatingPix,
                        modifier = Modifier.weight(1f),
                        onAction = onPix,
                    )
                    PaymentAction(
                        icon = Icons.Outlined.PictureAsPdf,
                        title = "Abrir boleto",
                        caption = "Visualizar PDF",
                        enabled = invoice.boletoPath != null,
                        modifier = Modifier.weight(1f),
                    ) { onOpenBoleto(invoice) }
                }
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

/** Copy/open quick action — disabled look when the API carries no value
 * for it on this invoice; `loading` swaps the icon for a spinner
 * (on-demand PIX generation in flight). */
@Composable
private fun PaymentAction(
    icon: ImageVector,
    title: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onAction: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !loading, onClick = onAction)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.padding(Spacing.xs).size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EndOfHistory() {
    Text(
        text = "Você viu todas as faturas",
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
    withPayment: Boolean = false,
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
    barcode = if (withPayment) "23793.38128 60007.827136 95000.063305 1 99100000123456" else null,
    pixCode = if (withPayment) "00020126-pix-copia-e-cola" else null,
    canGeneratePix = false,
    boletoPath = if (withPayment) "https://demo.sgp.net.br/boleto/$id" else null,
)

private val previewUiState = BillsUiState(
    loading = false,
    invoices = listOf(
        previewInvoice(1, InvoiceStatus.OPEN, LocalDate.now().plusDays(5), withPayment = true),
        previewInvoice(2, InvoiceStatus.OPEN, LocalDate.now().minusDays(2), withPayment = true),
        previewInvoice(3, InvoiceStatus.PAID, LocalDate.now().minusMonths(1)),
        previewInvoice(4, InvoiceStatus.CANCELLED, LocalDate.now().minusMonths(1).minusDays(3)),
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
            onCopyValue = {},
            onOpenBoleto = {},
            onPix = {},
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
            onCopyValue = {},
            onOpenBoleto = {},
            onPix = {},
        )
    }
}
