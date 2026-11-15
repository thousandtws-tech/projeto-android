package com.sgp.cliente.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DocumentFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession

private val OptionCardShape = RoundedCornerShape(28.dp)
private val IconBoxShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenPreferences: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPersonalData by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Minha conta") }) },
        modifier = modifier,
    ) { padding ->
        SgpContentFrame(Modifier.fillMaxSize().padding(padding)) {
            ProfileContent(
                session = uiState.session,
                onShowPersonalData = { showPersonalData = true },
                onOpenPreferences = onOpenPreferences,
                onOpenPrivacy = onOpenPrivacy,
                onLogout = viewModel::logout,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showPersonalData) {
        PersonalDataSheet(
            session = uiState.session,
            onDismiss = { showPersonalData = false },
        )
    }
}

@Composable
internal fun ProfileContent(
    session: CustomerSession?,
    onShowPersonalData: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Do seu jeito.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Seus dados e preferências, sempre à mão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            session?.let { TitularCard(it) }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ProfileOption(
                    icon = Icons.Filled.Person,
                    title = "Dados pessoais",
                    subtitle = "Seus contatos e informações.",
                    onClick = onShowPersonalData,
                )
                ProfileOption(
                    icon = Icons.Filled.Tune,
                    title = "Preferências",
                    subtitle = "Avisos e conexão com o provedor.",
                    onClick = onOpenPreferences,
                )
                ProfileOption(
                    icon = Icons.Filled.Shield,
                    title = "Privacidade",
                    subtitle = "Seus dados e seus direitos.",
                    onClick = onOpenPrivacy,
                )
            }
        }

        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(
                text = "Sair da conta",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TitularCard(session: CustomerSession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OptionCardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "TITULAR DA CONTA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = session.customerName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            session.selectedContract?.let { contract ->
                Text(
                    text = "Contrato ${contract.id} · ${contract.internetPlan ?: contract.status.trim()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = OptionCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = IconBoxShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(Spacing.xs),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalDataSheet(
    session: CustomerSession?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "Dados pessoais",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PersonalDataRow("Nome", session?.customerName.orEmpty())
            PersonalDataRow("CPF/CNPJ", DocumentFormatter.display(session?.cpfCnpj))
            session?.selectedContract?.let { contract ->
                PersonalDataRow("Contrato", "#${contract.id} — ${contract.status.trim()}")
                contract.internetPlan?.let { PersonalDataRow("Plano", it) }
                PersonalDataRow("Vencimento", "Todo dia ${contract.dueDay}")
            }
        }
    }
}

@Composable
private fun PersonalDataRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val previewSession = CustomerSession(
    credentials = Credentials(login = "maria@exemplo.com", password = ""),
    customerName = "Maria Souza",
    cpfCnpj = "12345678900",
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
)

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun ProfileContentPreview() {
    SgpTheme {
        ProfileContent(
            session = previewSession,
            onShowPersonalData = {},
            onOpenPreferences = {},
            onOpenPrivacy = {},
            onLogout = {},
        )
    }
}
