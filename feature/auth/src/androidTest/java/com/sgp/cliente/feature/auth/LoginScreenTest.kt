package com.sgp.cliente.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [LoginContent]. The composable is driven by a hoisted
 * [LoginUiState] so no ViewModel/Hilt graph is required.
 */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setLoginContent(
        initialState: LoginUiState = LoginUiState(),
        onSubmit: () -> Unit = {},
    ) {
        var state by mutableStateOf(initialState)
        composeRule.setContent {
            SgpTheme {
                LoginContent(
                    uiState = state,
                    onLoginChange = { state = state.copy(login = it) },
                    onPasswordChange = { state = state.copy(password = it) },
                    onSubmit = onSubmit,
                    supportWhatsApp = "",
                )
            }
        }
    }

    @Test
    fun usernameAndPasswordFieldsAreDisplayed() {
        setLoginContent()

        composeRule.onNodeWithTag(LoginTestTags.UsernameField).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.PasswordField).assertIsDisplayed()
        composeRule.onNodeWithText("CPF, CNPJ ou usuário").assertIsDisplayed()
        composeRule.onNodeWithText("Senha da central").assertIsDisplayed()
    }

    @Test
    fun submitButtonIsDisabledWhileFieldsAreEmpty() {
        setLoginContent()

        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsNotEnabled()
    }

    @Test
    fun typingBothCredentialsEnablesSubmitButton() {
        setLoginContent()

        composeRule.onNodeWithTag(LoginTestTags.UsernameField)
            .performTextInput("52998224725")
        composeRule.onNodeWithTag(LoginTestTags.PasswordField)
            .performTextInput("secret")

        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsEnabled()
    }

    @Test
    fun formErrorDisplaysTheErrorBanner() {
        setLoginContent(
            initialState = LoginUiState(
                login = "52998224725",
                password = "wrong",
                formError = AppError.Unauthorized,
            ),
        )

        composeRule.onNodeWithTag(LoginTestTags.ErrorBanner).assertIsDisplayed()
        composeRule
            .onNodeWithText("Sessão expirada ou credenciais inválidas. Entre novamente.")
            .assertIsDisplayed()
    }

    @Test
    fun fieldErrorDisplaysTheInlineMessage() {
        setLoginContent(
            initialState = LoginUiState(loginError = "Informe o CPF/CNPJ ou usuário"),
        )

        composeRule.onNodeWithText("Informe o CPF/CNPJ ou usuário").assertIsDisplayed()
    }

    @Test
    fun loadingStateShowsIndicatorAndDisablesSubmitButton() {
        setLoginContent(
            initialState = LoginUiState(
                login = "52998224725",
                password = "secret",
                loading = true,
            ),
        )

        // The progress indicator replaces the "Entrar" label while loading.
        composeRule.onAllNodesWithText("Entrar").assertCountEquals(0)
        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsNotEnabled()
    }
}
