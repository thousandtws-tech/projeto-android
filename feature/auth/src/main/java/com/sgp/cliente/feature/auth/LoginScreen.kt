package com.sgp.cliente.feature.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.designsystem.components.BrandLogo
import com.sgp.cliente.core.designsystem.components.SgpPasswordField
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpTextField
import com.sgp.cliente.core.designsystem.components.toUserMessage
import com.sgp.cliente.core.designsystem.theme.BrandDeep
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing

internal object LoginTestTags {
    const val UsernameField = "login_username_field"
    const val PasswordField = "login_password_field"
    const val SubmitButton = "login_submit_button"
    const val ErrorBanner = "login_error_banner"
    const val LoginTab = "login_tab_login"
    const val SignUpTab = "login_tab_signup"
}

private const val TAB_LOGIN = 0
private const val TAB_SIGNUP = 1

private const val ACCESS_HELP_MESSAGE =
    "Olá! Não sei meu acesso ao app JD FIBRA. Pode me ajudar?"

private const val HERO_WEIGHT = 0.36f
private const val SHEET_WEIGHT = 0.64f
private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    supportWhatsApp: String = "",
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        onLoginChange = viewModel::onLoginChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        supportWhatsApp = supportWhatsApp,
        modifier = modifier,
    )
}

@Composable
internal fun LoginContent(
    uiState: LoginUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    supportWhatsApp: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_LOGIN) }
    var signUpName by rememberSaveable { mutableStateOf("") }
    var signUpPhone by rememberSaveable { mutableStateOf("") }
    var signUpStreet by rememberSaveable { mutableStateOf("") }
    val showSupport = supportWhatsApp.isNotBlank()

    // The brand band bleeds behind the status bar — light icons keep the
    // clock/battery readable on it. enableEdgeToEdge() re-asserts dark
    // icons on every layout pass, so the flag is re-applied through a
    // GlobalLayoutListener while this screen is visible.
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = ViewCompat.getWindowInsetsController(view)
            ?: return@DisposableEffect onDispose {}
        val previous = controller.isAppearanceLightStatusBars
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            controller.isAppearanceLightStatusBars = false
        }
        view.rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        controller.isAppearanceLightStatusBars = false
        onDispose {
            view.rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            controller.isAppearanceLightStatusBars = previous
        }
    }

    Surface(
        color = BrandDeep,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandHero(modifier = Modifier.fillMaxWidth().weight(HERO_WEIGHT))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = SheetShape,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(SHEET_WEIGHT)
                    .clip(SheetShape),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SheetHandle()

                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        if (showSupport) {
                            AccessRecoveryLink(
                                onClick = {
                                    openWhatsApp(
                                        context,
                                        supportWhatsApp,
                                        ACCESS_HELP_MESSAGE,
                                    )
                                },
                            )
                            LoginSignUpTabs(
                                selectedTab = selectedTab,
                                onSelect = { selectedTab = it },
                            )
                        }

                        if (selectedTab == TAB_LOGIN) {
                            LoginForm(
                                uiState = uiState,
                                onLoginChange = onLoginChange,
                                onPasswordChange = onPasswordChange,
                                onSubmit = onSubmit,
                            )
                        } else {
                            SignUpForm(
                                name = signUpName,
                                onNameChange = { signUpName = it },
                                phone = signUpPhone,
                                onPhoneChange = { signUpPhone = it },
                                street = signUpStreet,
                                onStreetChange = { signUpStreet = it },
                                onContactAttendant = {
                                    openWhatsApp(
                                        context,
                                        supportWhatsApp,
                                        buildSignUpMessage(
                                            signUpName,
                                            signUpPhone,
                                            signUpStreet,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Full-bleed brand band above the sheet: app mark in a tonal circle,
 * title and subtitle — with the app's signature rings bleeding off the
 * top-right corner. */
@Composable
private fun BrandHero(modifier: Modifier = Modifier) {
    Box(modifier = modifier.statusBarsPadding(), contentAlignment = Alignment.Center) {
        HeroDecorations()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogo(
                contentDescription = "Logo do provedor",
                modifier = Modifier.height(120.dp),
            )
            Text(
                text = "Central do Cliente",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = Spacing.md),
            )
            Text(
                text = "Acesse sua conta do provedor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = Spacing.xxs),
            )
        }
    }
}

/** Grab bar at the sheet's top edge — the bottom-sheet affordance users
 * recognize from modern apps. */
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = Spacing.sm)
            .width(40.dp)
            .height(4.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                CircleShape,
            ),
    )
}

/** Light circles bleeding off the top-right corner — same motif as the
 * dashboard hero so login reads as part of the same product. */
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

@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
            text = "Use o CPF/CNPJ ou usuário e a senha cadastrados na JD FIBRA.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun SignUpForm(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    street: String,
    onStreetChange: (String) -> Unit,
    onContactAttendant: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SgpTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nome completo",
            placeholder = "Insira seu nome",
        )

        SgpTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Telefone para contato",
            placeholder = "(DDD) 9 9999-9999",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        SgpTextField(
            value = street,
            onValueChange = onStreetChange,
            label = "Nome da sua rua",
            placeholder = "Ex.: Rua das Flores",
        )

        SgpPrimaryButton(
            text = "Falar com atendente",
            onClick = onContactAttendant,
            enabled = name.isNotBlank() || phone.isNotBlank(),
            modifier = Modifier.padding(top = Spacing.xs),
        )

        Text(
            text = "Informe seus dados e um atendente entra em contato pelo WhatsApp.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        )
    }
}

@Composable
private fun AccessRecoveryLink(onClick: () -> Unit) {
    val linkColor = MaterialTheme.colorScheme.secondary
    Text(
        text = buildAnnotatedString {
            append("Caso não saiba seu acesso, ")
            withStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append("clique aqui.")
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xxs),
    )
}

/** Pill-style segmented control — a tonal track with a filled primary
 * thumb sliding between "Login" and "Contratar". */
@Composable
private fun LoginSignUpTabs(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            SegmentItem(
                text = "Login",
                selected = selectedTab == TAB_LOGIN,
                onClick = { onSelect(TAB_LOGIN) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(LoginTestTags.LoginTab),
            )
            SegmentItem(
                text = "Contratar",
                selected = selectedTab == TAB_SIGNUP,
                onClick = { onSelect(TAB_SIGNUP) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(LoginTestTags.SignUpTab),
            )
        }
    }
}

@Composable
private fun SegmentItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        shape = CircleShape,
        modifier = modifier.clip(CircleShape).clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
    }
}

private fun buildSignUpMessage(name: String, phone: String, street: String): String =
    buildString {
        append("Olá! Quero contratar um plano de internet.")
        if (name.isNotBlank()) append("\nNome: ").append(name.trim())
        if (phone.isNotBlank()) append("\nTelefone: ").append(phone.trim())
        if (street.isNotBlank()) append("\nRua: ").append(street.trim())
    }

private fun openWhatsApp(context: Context, number: String, message: String) {
    val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
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
            supportWhatsApp = "",
        )
    }
}

@Preview(showBackground = true, name = "Login — Com suporte")
@Composable
private fun LoginContentSupportPreview() {
    SgpTheme {
        LoginContent(
            uiState = LoginUiState(login = "52998224725", password = "secret"),
            onLoginChange = {},
            onPasswordChange = {},
            onSubmit = {},
            supportWhatsApp = "5573999999999",
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
            supportWhatsApp = "5573999999999",
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
            supportWhatsApp = "",
        )
    }
}
