package com.sgp.cliente.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpTextField
import com.sgp.cliente.core.designsystem.components.SgpTooltipButton
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servidor do provedor") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = "Informe o endereço do sistema do seu provedor (ex.: https://provedor.exemplo.com.br).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SgpTextField(
                    value = uiState.baseUrl,
                    onValueChange = viewModel::onUrlChange,
                    label = "URL do servidor",
                    errorMessage = uiState.fieldError,
                    enabled = !uiState.checking,
                )

                uiState.error?.let { error ->
                    Text(
                        text = error.toUserMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                uiState.selectedCentral?.let { central ->
                    Text(
                        text = "Central conectada: $central",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                SgpPrimaryButton(
                    text = "Salvar e conectar",
                    onClick = viewModel::save,
                    enabled = uiState.baseUrl.isNotBlank(),
                    loading = uiState.checking,
                )
            }
        }
    }
}
