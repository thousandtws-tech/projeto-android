package com.sgp.cliente.feature.dashboard

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.contract.CheckServiceStatusUseCase
import com.sgp.cliente.domain.usecase.invoice.GetOpenInvoicesUseCase
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeSession: ObserveSessionUseCase = mockk()
    private val getOpenInvoices: GetOpenInvoicesUseCase = mockk()
    private val checkServiceStatus: CheckServiceStatusUseCase = mockk()
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)
    private lateinit var viewModel: DashboardViewModel

    private val contract = Contract(
        id = 7,
        status = "Ativo",
        razaoSocial = "Maria",
        cpfCnpj = "52998224725",
        dueDay = 10,
        internetPlan = null,
        internetPlanValue = null,
        internetPlanId = null,
    )
    private val session = CustomerSession(
        credentials = Credentials("52998224725", "secret"),
        customerName = "Maria",
        contracts = listOf(contract),
        selectedContractId = 7,
    )
    private val open = OpenInvoices(
        overdue = listOf(invoice(1)),
        upcoming = listOf(invoice(2)),
    )

    private fun invoice(id: Int) = Invoice(
        id = id,
        documentNumber = null,
        statusId = 1,
        statusLabel = "Em aberto",
        dueDate = LocalDate.of(2026, 3, 10),
        effectiveDueDate = null,
        amount = 100.0,
        correctedAmount = null,
        paidAt = null,
        barcode = null,
        pixCode = null,
        canGeneratePix = false,
        boletoPath = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeSession() } returns flowOf(SessionState.LoggedIn(session))
        coEvery { checkServiceStatus() } returns
            AppResult.success(ServiceStatus(1, isOnline = true, message = "Online"))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `state starts loading before the first load completes`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )

        assertTrue(viewModel.uiState.value.loading)
    }

    @Test
    fun `successful load exposes the open invoices and clears loading`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertFalse(state.refreshing)
        assertNull(state.error)
        assertEquals(listOf(1, 2), state.all.map { it.id })
        assertEquals(listOf(1), state.openInvoices.overdue.map { it.id })
        assertEquals(listOf(2), state.openInvoices.upcoming.map { it.id })
    }

    @Test
    fun `logged-in session is exposed together with its selected contract`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(session, viewModel.uiState.value.session)
        assertEquals(7, viewModel.uiState.value.contract?.id)
    }

    @Test
    fun `logged-out session leaves the dashboard session empty`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedOut)
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.session)
        assertNull(viewModel.uiState.value.contract)
    }

    @Test
    fun `load failure surfaces the typed error`() = runTest {
        coEvery { getOpenInvoices(any()) } returns
            AppResult.failure(AppError.NetworkUnavailable)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertFalse(state.refreshing)
        assertEquals(AppError.NetworkUnavailable, state.error)
    }

    @Test
    fun `the initial load uses the cached path`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { getOpenInvoices(false) }
        coVerify(exactly = 0) { getOpenInvoices(true) }
    }

    @Test
    fun `refresh forces a remote reload and toggles the refreshing flag`() = runTest {
        val gate = CompletableDeferred<AppResult<OpenInvoices>>()
        coEvery { getOpenInvoices(false) } returns AppResult.success(open)
        coEvery { getOpenInvoices(true) } coAnswers { gate.await() }
        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.refreshing)
        coVerify(exactly = 1) { getOpenInvoices(true) }

        gate.complete(AppResult.success(open))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.refreshing)
    }

    @Test
    fun `failed refresh keeps the loaded invoices and surfaces the error`() = runTest {
        coEvery { getOpenInvoices(false) } returns AppResult.success(open)
        coEvery { getOpenInvoices(true) } returns AppResult.failure(AppError.Server)
        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppError.Server, state.error)
        assertFalse(state.refreshing)
        assertEquals(listOf(1, 2), state.all.map { it.id })
    }

    @Test
    fun `service status is exposed for the connectivity pill`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.serviceStatus?.isOnline)
    }

    @Test
    fun `service status failure is silent — pill stays hidden`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { checkServiceStatus() } returns AppResult.failure(AppError.Server)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.serviceStatus)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `logout delegates to the use case`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { logoutUseCase() }
    }

    @Test
    fun `current invoice picks the first overdue over upcoming`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = DashboardViewModel(
            observeSession, getOpenInvoices, checkServiceStatus, logoutUseCase,
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentInvoice?.id)
    }
}
