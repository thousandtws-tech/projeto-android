package com.sgp.cliente.feature.auth

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.usecase.auth.LoginUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val loginUseCase: LoginUseCase = mockk()
    private lateinit var viewModel: LoginViewModel

    private val session = CustomerSession(
        credentials = Credentials("52998224725", "secret"),
        customerName = "Maria",
        contracts = emptyList(),
        selectedContractId = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank login surfaces field error and never calls the API`() = runTest {
        viewModel.onLoginChange("   ")
        viewModel.onPasswordChange("x")
        viewModel.submit()

        assertEquals("Informe o CPF/CNPJ ou usuário", viewModel.uiState.value.loginError)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `blank password surfaces field error`() = runTest {
        viewModel.onLoginChange("52998224725")
        viewModel.onPasswordChange("")
        viewModel.submit()
        assertEquals("Informe a senha", viewModel.uiState.value.passwordError)
    }

    @Test
    fun `username identifier is accepted (central login, not only CPF)`() = runTest {
        // `modo_autenticacao = 1` accepts CPF, CNPJ or the central username.
        coEvery { loginUseCase("6zta35f0", "secret") } returns AppResult.success(session)

        viewModel.onLoginChange("6zta35f0")
        viewModel.onPasswordChange("secret")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.loginError)
        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `successful login clears loading without form error`() = runTest {
        coEvery { loginUseCase("52998224725", "secret") } returns AppResult.success(session)

        viewModel.onLoginChange("52998224725")
        viewModel.onPasswordChange("secret")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun `API failure surfaces form error`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns AppResult.failure(AppError.Unauthorized)

        viewModel.onLoginChange("52998224725")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppError.Unauthorized, viewModel.uiState.value.formError)
        assertFalse(viewModel.uiState.value.loading)
    }
}
