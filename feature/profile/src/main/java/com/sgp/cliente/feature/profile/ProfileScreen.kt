package com.sgp.cliente.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.format.DocumentFormatter
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpPasswordField
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.BrandDeep
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
    var showChangePassword by remember { mutableStateOf(false) }
    var passwordChanged by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.passwordChange) {
        if (uiState.passwordChange is PasswordChangeState.Success) {
            showChangePassword = false
            passwordChanged = true
            viewModel.resetPasswordChange()
        }
    }

    SgpContentFrame(modifier.fillMaxSize()) {
        ProfileContent(
            session = uiState.session,
            onShowPersonalData = { showPersonalData = true },
            onOpenPreferences = onOpenPreferences,
            onOpenPrivacy = onOpenPrivacy,
            onOpenChangePassword = { showChangePassword = true },
            onLogout = viewModel::logout,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showPersonalData) {
        PersonalDataSheet(
            session = uiState.session,
            onDismiss = { showPersonalData = false },
        )
    }

    if (showChangePassword) {
        ChangePasswordSheet(
            state = uiState.passwordChange,
            onSubmit = viewModel::changePassword,
            onDismiss = {
                showChangePassword = false
                viewModel.resetPasswordChange()
            },
        )
    }

    if (passwordChanged) {
        AlertDialog(
            onDismissRequest = { passwordChanged = false },
            title = { Text("Senha alterada") },
            text = { Text("Sua senha de acesso foi atualizada com sucesso.") },
            confirmButton = {
                TextButton(onClick = { passwordChanged = false }) { Text("OK") }
            },
        )
    }
}

@Composable
internal fun ProfileContent(
    session: CustomerSession?,
    onShowPersonalData: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        session?.let { TitularHeroCard(it) }

        OptionsCard {
            ProfileOption(
                icon = Icons.Filled.Person,
                title = "Dados pessoais",
                subtitle = "Seus contatos e informações.",
                onClick = onShowPersonalData,
            )
            OptionDivider()
            ProfileOption(
                icon = Icons.Filled.Tune,
                title = "Preferências",
                subtitle = "Avisos e conexão com o provedor.",
                onClick = onOpenPreferences,
            )
            OptionDivider()
            ProfileOption(
                icon = Icons.Filled.Shield,
                title = "Privacidade",
                subtitle = "Seus dados e seus direitos.",
                onClick = onOpenPrivacy,
            )
            OptionDivider()
            ProfileOption(
                icon = Icons.Filled.Lock,
                title = "Alterar senha",
                subtitle = "Troque a senha de acesso à Central.",
                onClick = onOpenChangePassword,
            )
        }

        LogoutOption(onClick = onLogout)

        Text(
            text = "Central do Cliente · J D FIBRA",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TitularHeroCard(session: CustomerSession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OptionCardShape,
        color = BrandDeep,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(180.dp)
                    .offset(x = 70.dp, y = (-64).dp)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .offset(x = 58.dp, y = 96.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarInitials(session.customerName)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(
                            text = "TITULAR DA CONTA",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = session.customerName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
                session.selectedContract?.let { contract ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "Contrato ${contract.id} · ${contract.internetPlan ?: contract.status.trim()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarInitials(name: String) {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, Color.White),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initialsOf(name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private fun initialsOf(name: String): String {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .mapNotNull { token -> token.firstOrNull(Char::isLetter)?.uppercaseChar() }
    return when {
        initials.isEmpty() -> "?"
        initials.size == 1 -> initials[0].toString()
        else -> "${initials.first()}${initials.last()}"
    }
}

@Composable
private fun OptionsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OptionCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(content = content)
    }
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = IconBoxShape,
            color = BrandDeep,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
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

@Composable
private fun LogoutOption(onClick: () -> Unit) {
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
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.xs),
                )
            }
            Text(
                text = "Sair da conta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(
    state: PasswordChangeState,
    onSubmit: (current: String, new: String, confirm: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

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
                text = "Alterar senha",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SgpPasswordField(
                value = current,
                onValueChange = { current = it },
                label = "Senha atual",
                enabled = state !is PasswordChangeState.Loading,
            )
            SgpPasswordField(
                value = new,
                onValueChange = { new = it },
                label = "Nova senha",
                enabled = state !is PasswordChangeState.Loading,
            )
            SgpPasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirmar nova senha",
                enabled = state !is PasswordChangeState.Loading,
            )
            (state as? PasswordChangeState.Error)?.let {
                Text(
                    text = it.error.toUserMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SgpPrimaryButton(
                text = "Salvar nova senha",
                onClick = { onSubmit(current, new, confirm) },
                loading = state is PasswordChangeState.Loading,
            )
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
            address = "Rua das Flores, 123 - Centro",
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
            onOpenChangePassword = {},
            onLogout = {},
        )
    }
}
