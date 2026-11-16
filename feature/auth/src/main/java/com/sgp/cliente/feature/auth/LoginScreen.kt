package com.sgp.cliente.feature.auth

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
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.designsystem.components.SgpPasswordField
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpTextField
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing

internal object LoginTestTags {
    const val UsernameField = "login_username_field"
    const val PasswordField = "login_password_field"
    const val SubmitButton = "login_submit_button"
    const val ErrorBanner = "login_error_banner"
}

@Composable
fun LoginScreen(
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        onLoginChange = viewModel::onLoginChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onOpenPrivacy = onOpenPrivacy,
        modifier = modifier,
    )
}

@Composable
internal fun LoginContent(
    uiState: LoginUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                LoginHeader()

                SgpTextField(
                    value = uiState.login,
                    onValueChange = onLoginChange,
                    label = "CPF, CNPJ ou usuário",
                    placeholder = "Seu login da Central",
                    errorMessage = uiState.loginError,
                    enabled = !uiState.loading,
                    modifier = Modifier.testTag(LoginTestTags.UsernameField),
                )

                SgpPasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = "Senha da central",
                    errorMessage = uiState.passwordError,
                    enabled = !uiState.loading,
                    modifier = Modifier.testTag(LoginTestTags.PasswordField),
                )

                uiState.formError?.let { error ->
                    FormErrorBanner(
                        message = error.toUserMessage(),
                        modifier = Modifier.testTag(LoginTestTags.ErrorBanner),
                    )
                }

                SgpPrimaryButton(
                    text = "Entrar",
                    onClick = onSubmit,
                    enabled = uiState.canSubmit,
                    loading = uiState.loading,
                    modifier = Modifier
                        .padding(top = Spacing.xs)
                        .testTag(LoginTestTags.SubmitButton),
                )

                Text(
                    text = "Use o CPF/CNPJ ou usuário e a senha cadastrados na Central do Assinante do seu provedor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )

                TextButton(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "Política de Privacidade",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.sm).widthIn(min = 56.dp),
        )
        Text(
            text = "Central do Cliente",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Acesse sua conta do provedor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
    }
}

@Composable
private fun FormErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

@Preview(showBackground = true, name = "Login — Vazio")
@Composable
private fun LoginContentEmptyPreview() {
    SgpTheme {
        LoginContent(
            uiState = LoginUiState(),
            onLoginChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onOpenPrivacy = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — Preenchido")
@Composable
private fun LoginContentFilledPreview() {
    SgpTheme {
        LoginContent(
            uiState = LoginUiState(login = "52998224725", password = "secret"),
            onLoginChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onOpenPrivacy = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — Erro")
@Composable
private fun LoginContentErrorPreview() {
    SgpTheme {
        LoginContent(
            uiState = LoginUiState(
                login = "52998224725",
                password = "wrong",
                formError = AppError.Unauthorized,
            ),
            onLoginChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onOpenPrivacy = {},
        )
    }
}

@Preview(showBackground = true, name = "Login — Carregando")
@Composable
private fun LoginContentLoadingPreview() {
    SgpTheme {
        LoginContent(
            uiState = LoginUiState(
                login = "52998224725",
                password = "secret",
                loading = true,
            ),
            onLoginChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onOpenPrivacy = {},
        )
    }
}
