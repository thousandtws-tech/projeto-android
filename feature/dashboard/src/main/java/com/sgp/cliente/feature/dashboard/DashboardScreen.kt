package com.sgp.cliente.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.sgp.cliente.core.designsystem.components.SgpErrorView
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSecondaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonDashboard
import com.sgp.cliente.core.designsystem.components.SgpStatusBadge
import com.sgp.cliente.core.designsystem.theme.BrandDeep
import com.sgp.cliente.core.designsystem.theme.LayoutTokens
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.core.designsystem.theme.StatusPaid
import com.sgp.cliente.core.designsystem.theme.StatusPaidContainer
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
    onOpenPromise: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { padding ->
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
                    else -> DashboardContent(
                        uiState = uiState,
                        onOpenInvoice = onOpenInvoice,
                        onOpenBills = onOpenBills,
                        onOpenPromise = onOpenPromise,
                        onOpenContracts = onOpenContracts,
                        onOpenProfile = onOpenProfile,
                        onLogout = viewModel::logout,
                    )
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
    onOpenPromise: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val today = LocalDate.now()
    if (currentWindowWidthSizeClass() == WindowWidthSizeClass.Compact) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            dashboardItems(
                uiState, today, onOpenInvoice, onOpenBills, onOpenPromise,
                onOpenContracts, onOpenProfile, onLogout,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(LayoutTokens.gridItemMinWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            dashboardItems(
                uiState, today, onOpenInvoice, onOpenBills, onOpenPromise,
                onOpenContracts, onOpenProfile, onLogout,
            )
        }
    }
}

private fun LazyListScope.dashboardItems(
    uiState: DashboardUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
    onOpenPromise: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    item {
        HeroCard(uiState, onOpenContracts, onLogout)
    }

    item {
        QuickActionsRow(uiState, onOpenInvoice, onOpenBills, onOpenProfile)
    }

    item { InvoicesHeader(onOpenBills) }

    val current = uiState.currentInvoice
    if (current != null) {
        item {
            InvoicesCarousel(
                current = current,
                next = uiState.all.filter { it.id != current.id },
                today = today,
                onOpenInvoice = onOpenInvoice,
            )
        }
    } else {
        item { OpenInvoicesEmpty() }
    }

    item {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SgpPrimaryButton(text = "Histórico de pagamentos", onClick = onOpenBills)
            SgpSecondaryButton(text = "Desbloqueio de confiança", onClick = onOpenPromise)
        }
    }
}

private fun LazyGridScope.dashboardItems(
    uiState: DashboardUiState,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
    onOpenPromise: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    // Non-card elements stretch across all columns; invoice cards fill cells.
    item(span = { GridItemSpan(maxLineSpan) }) {
        HeroCard(uiState, onOpenContracts, onLogout)
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        QuickActionsRow(uiState, onOpenInvoice, onOpenBills, onOpenProfile)
    }

    item(span = { GridItemSpan(maxLineSpan) }) { InvoicesHeader(onOpenBills) }

    val current = uiState.currentInvoice
    if (current != null) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            InvoicesCarousel(
                current = current,
                next = uiState.all.filter { it.id != current.id },
                today = today,
                onOpenInvoice = onOpenInvoice,
            )
        }
    } else {
        item(span = { GridItemSpan(maxLineSpan) }) { OpenInvoicesEmpty() }
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SgpPrimaryButton(text = "Histórico de pagamentos", onClick = onOpenBills)
            SgpSecondaryButton(text = "Desbloqueio de confiança", onClick = onOpenPromise)
        }
    }
}

/** Rounded brand container (margins + big radius): greeting, plan info
 * directly on the tint and status/contract pills — mirrors the reference. */
@Composable
private fun HeroCard(
    uiState: DashboardUiState,
    onOpenContracts: () -> Unit,
    onLogout: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = shape,
        modifier = Modifier.fillMaxWidth().clip(shape),
    ) {
        Box {
            HeroDecorations()
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Olá, ${uiState.session?.firstName ?: "cliente"}.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onLogout) {
                        Text("Sair", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                uiState.contract?.let { contract ->
                    PlanBlock(
                        contract = contract,
                        serviceStatus = uiState.serviceStatus,
                        showSwitch = uiState.hasMultipleContracts,
                        onOpenContracts = onOpenContracts,
                    )
                }
            }
        }
    }
}

/** Light circles bleeding off the top-right corner — decorative, like the
 * reference's ring shapes. Surface's clip keeps them inside the radius. */
@Composable
private fun BoxScope.HeroDecorations() {
    val tint = MaterialTheme.colorScheme.onPrimary
    Box(
        Modifier
            .size(180.dp)
            .align(Alignment.TopEnd)
            .offset(x = 70.dp, y = (-64).dp)
            .background(tint.copy(alpha = 0.08f), CircleShape),
    )
    Box(
        Modifier
            .size(120.dp)
            .align(Alignment.TopEnd)
            .offset(x = 58.dp, y = 96.dp)
            .border(16.dp, tint.copy(alpha = 0.10f), CircleShape),
    )
}

/** Plan block directly on the hero tint — no inner card, like the reference. */
@Composable
private fun PlanBlock(
    contract: Contract,
    serviceStatus: ServiceStatus?,
    showSwitch: Boolean,
    onOpenContracts: () -> Unit,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(modifier = Modifier.padding(top = Spacing.md)) {
        Text(
            text = "Meu plano",
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary.copy(alpha = 0.75f),
        )
        Text(
            text = contract.internetPlan ?: "Contrato ${contract.id}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onPrimary,
        )
        Text(
            text = "Vencimento dia ${contract.dueDay}" +
                (contract.internetPlanValue?.let { " • ${MoneyFormatter.brl(it)}/mês" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = onPrimary.copy(alpha = 0.85f),
        )
        Row(
            modifier = Modifier.padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(serviceStatus, contract)
            if (showSwitch) {
                SwitchContractPill(onOpenContracts)
            }
        }
    }
}

/** Tone-on-tone pill on the hero: deep brand when offline, mint when online. */
@Composable
private fun StatusPill(serviceStatus: ServiceStatus?, contract: Contract) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val (label, container, content) = when {
        serviceStatus != null && serviceStatus.isOnline ->
            Triple("Você está online", StatusPaidContainer, StatusPaid)
        serviceStatus != null ->
            Triple("Você está offline", BrandDeep, Color.White)
        else -> Triple(
            contract.status,
            onPrimary.copy(alpha = 0.18f),
            onPrimary,
        )
    }
    Surface(color = container, shape = CircleShape) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** White pill button on the hero — the reference's "Trocar Contrato". */
@Composable
private fun SwitchContractPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
    ) {
        Text(
            text = "Trocar Contrato",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** "Sobre sua conta" — three quick-action cards wired to real destinations. */
@Composable
private fun QuickActionsRow(
    uiState: DashboardUiState,
    onOpenInvoice: (Int) -> Unit,
    onOpenBills: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
        Text(
            text = "Sobre minha conta",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            QuickActionCard(
                icon = Icons.Filled.QrCode2,
                label = "Pagar com PIX",
                onClick = {
                    uiState.currentInvoice?.let { onOpenInvoice(it.id) } ?: onOpenBills()
                },
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = "Segunda via",
                onClick = onOpenBills,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.Person,
                label = "Meus dados",
                onClick = onOpenProfile,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun InvoicesHeader(onOpenBills: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Minhas faturas",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenBills) {
            Text("Ver todas")
        }
    }
}

/** M3 uncontained carousel: a full-color hero card for the current invoice
 * followed by compact tonal cards — the peeked edge is the affordance,
 * matching the reference's swipable invoice cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoicesCarousel(
    current: Invoice,
    next: List<Invoice>,
    today: LocalDate,
    onOpenInvoice: (Int) -> Unit,
) {
    val itemCount = next.size + 1
    val state = rememberCarouselState { itemCount }
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = 300.dp,
        itemSpacing = Spacing.sm,
        modifier = Modifier.fillMaxWidth(),
    ) { index ->
        if (index == 0) {
            CurrentInvoiceHeroCard(
                invoice = current,
                today = today,
                onClick = { onOpenInvoice(current.id) },
                modifier = Modifier.maskClip(RoundedCornerShape(24.dp)),
            )
        } else {
            val invoice = next[index - 1]
            NextInvoiceCard(
                invoice = invoice,
                today = today,
                onClick = { onOpenInvoice(invoice.id) },
                modifier = Modifier.maskClip(RoundedCornerShape(24.dp)),
            )
        }
    }
}

/** Full-color current invoice — the reference's highlighted payable card,
 * with an on-card "Pagar agora" action. */
@Composable
private fun CurrentInvoiceHeroCard(
    invoice: Invoice,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = invoice.isOverdue(today)
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth().height(160.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Fatura atual",
                    style = MaterialTheme.typography.labelMedium,
                    color = onPrimary.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f),
                )
                Surface(color = onPrimary.copy(alpha = 0.18f), shape = CircleShape) {
                    Text(
                        text = if (overdue) "Vencida" else "Em aberto",
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Text(
                text = MoneyFormatter.brl(invoice.displayAmount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onPrimary,
            )
            Text(
                text = (if (overdue) "Venceu em " else "Vence em ") +
                    DateFormatter.display(invoice.effectiveOrOriginalDueDate),
                style = MaterialTheme.typography.bodySmall,
                color = onPrimary.copy(alpha = 0.85f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom,
            ) {
                Surface(
                    onClick = onClick,
                    color = onPrimary,
                    shape = CircleShape,
                ) {
                    Text(
                        text = "Pagar agora",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Compact tonal card for the upcoming invoices in the carousel. */
@Composable
private fun NextInvoiceCard(
    invoice: Invoice,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val overdue = invoice.isOverdue(today)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth().height(160.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Vence em ${DateFormatter.display(invoice.effectiveOrOriginalDueDate)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = MoneyFormatter.brl(invoice.displayAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            SgpStatusBadge(
                text = if (overdue) "Vencida" else "Em aberto",
                kind = if (overdue) SgpBadgeKind.OVERDUE else SgpBadgeKind.OPEN,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
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

private val CustomerSession.firstName: String
    get() = customerName.trim().split("\\s+".toRegex()).firstOrNull()
        ?.lowercase()?.replaceFirstChar { it.titlecase() }
        ?: ""

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
    serviceStatus = ServiceStatus(statusCode = 1, isOnline = true, message = "Online"),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun DashboardContentCompactPreview() {
    SgpTheme {
        DashboardContent(
            uiState = previewUiState,
            onOpenInvoice = {},
            onOpenBills = {},
            onOpenPromise = {},
            onOpenContracts = {},
            onOpenProfile = {},
            onLogout = {},
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
            onOpenPromise = {},
            onOpenContracts = {},
            onOpenProfile = {},
            onLogout = {},
        )
    }
}
