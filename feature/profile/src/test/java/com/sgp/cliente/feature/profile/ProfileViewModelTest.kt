package com.sgp.cliente.feature.profile

import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.usecase.auth.ChangePasswordUseCase
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeSession: ObserveSessionUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk()
    private val changePasswordUseCase: ChangePasswordUseCase = mockk()
    private lateinit var viewModel: ProfileViewModel

    private val session = CustomerSession(
        credentials = Credentials("52998224725", "secret"),
        customerName = "Maria",
        contracts = listOf(
            Contract(
                id = 7,
                status = "Ativo",
                razaoSocial = "Maria",
                cpfCnpj = "52998224725",
                dueDay = 10,
                internetPlan = null,
                internetPlanValue = null,
                internetPlanId = null,
                address = null,
            ),
        ),
        selectedContractId = 7,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { logoutUseCase() } returns Unit
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `logged-in session is exposed on the ui state`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))

        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(session, viewModel.uiState.value.session)
    }

    @Test
    fun `logged-out state exposes no session`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedOut)

        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun `session going from logged-in to logged-out clears the exposed session`() = runTest {
        val sessionFlow = MutableStateFlow<SessionState>(SessionState.LoggedIn(session))
        every { observeSession() } returns sessionFlow
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(session, viewModel.uiState.value.session)

        sessionFlow.value = SessionState.LoggedOut
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
    }

    @Test
    fun `logout invokes the logout use case`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { logoutUseCase() }
    }

    @Test
    fun `changePassword with mismatched confirmation fails locally without calling the use case`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("old1", "new1", "new2")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.passwordChange is PasswordChangeState.Error)
        coVerify(exactly = 0) { changePasswordUseCase(any(), any()) }
    }

    @Test
    fun `changePassword with short new password fails locally`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("old1", "ab", "ab")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.passwordChange is PasswordChangeState.Error)
        coVerify(exactly = 0) { changePasswordUseCase(any(), any()) }
    }

    @Test
    fun `changePassword success exposes Success state`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))
        coEvery { changePasswordUseCase("old1", "newpass") } returns AppResult.success(Unit)
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("old1", "newpass", "newpass")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PasswordChangeState.Success, viewModel.uiState.value.passwordChange)
        coVerify(exactly = 1) { changePasswordUseCase("old1", "newpass") }
    }

    @Test
    fun `changePassword server error exposes Error state`() = runTest {
        every { observeSession() } returns MutableStateFlow(SessionState.LoggedIn(session))
        coEvery { changePasswordUseCase("old1", "newpass") } returns
            AppResult.failure(AppError.Api("Senha atual incorreta."))
        viewModel = ProfileViewModel(observeSession, logoutUseCase, changePasswordUseCase)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("old1", "newpass", "newpass")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.passwordChange is PasswordChangeState.Error)
    }
}
