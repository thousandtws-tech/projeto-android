package com.sgp.cliente.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSkeletonBox
import com.sgp.cliente.core.designsystem.components.SgpTooltipButton
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferências") },
                navigationIcon = {
                    SgpTooltipButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Voltar",
                        onClick = onBack,
                    )
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        SgpContentFrame(Modifier.fillMaxSize().padding(padding)) {
            PreferencesContent(
                uiState = uiState,
                onInvoiceAlertsChange = viewModel::setInvoiceAlerts,
                onTicketUpdatesChange = viewModel::setTicketUpdates,
                onSave = viewModel::save,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun PreferencesContent(
    uiState: PreferencesUiState,
    onInvoiceAlertsChange: (Boolean) -> Unit,
    onTicketUpdatesChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "Preferências",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Escolha como deseja acompanhar seus serviços.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.loading) {
            repeat(2) { SgpSkeletonBox(height = 88.dp, shape = RoundedCornerShape(12.dp)) }
        } else {
            PreferenceToggle(
                title = "Avisos de fatura",
                subtitle = "Receber alertas de vencimento",
                checked = uiState.invoiceAlerts,
                onCheckedChange = onInvoiceAlertsChange,
            )
            PreferenceToggle(
                title = "Atualizações de chamados",
                subtitle = "Receber novidades sobre o atendimento",
                checked = uiState.ticketUpdates,
                onCheckedChange = onTicketUpdatesChange,
            )
        }

        Text(
            text = "Suas preferências ficam salvas neste aparelho. Permissões do sistema serão solicitadas quando necessário.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SgpPrimaryButton(
            text = "Salvar preferências",
            onClick = onSave,
            loading = uiState.saving,
        )
    }
}

@Composable
private fun PreferenceToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        // The whole row is the switch: toggleable merges the title/subtitle
        // text into a single TalkBack node announced as "On/Off, Switch",
        // and tapping anywhere toggles. The inner Switch is display-only
        // (onCheckedChange = null) so it isn't a separate focus target.
        Row(
            modifier = Modifier
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun PreferencesContentPreview() {
    SgpTheme {
        PreferencesContent(
            uiState = PreferencesUiState(
                loading = false,
                invoiceAlerts = true,
                ticketUpdates = false,
            ),
            onInvoiceAlertsChange = {},
            onTicketUpdatesChange = {},
            onSave = {},
        )
    }
}
