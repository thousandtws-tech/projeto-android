package com.sgp.cliente.feature.bills

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.invoice.GetBoletoUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoicesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeSession: ObserveSessionUseCase = mockk()
    private val getInvoices: GetInvoicesUseCase = mockk()
    private val getBoleto: GetBoletoUseCase = mockk()
    private lateinit var viewModel: BillsViewModel

    private fun contract(id: Int) = Contract(
        id = id,
        status = "Ativo",
        razaoSocial = "Maria",
        cpfCnpj = "52998224725",
        dueDay = 10,
        internetPlan = null,
        internetPlanValue = null,
        internetPlanId = null,
        address = null,
    )

    private fun session(selectedId: Int) = CustomerSession(
        credentials = Credentials("52998224725", "secret"),
        customerName = "Maria",
        contracts = listOf(contract(1), contract(2)),
        selectedContractId = selectedId,
    )

    private fun invoice(
        id: Int = 42,
        boletoPath: String? = null,
    ) = Invoice(
        id = id,
        documentNumber = 46667140L + id,
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
        boletoPath = boletoPath,
    )

    private fun stubPage(items: List<Invoice>) {
        coEvery { getInvoices(any(), any(), any(), any()) } returns
            AppResult.success(PagedResult(items = items, offset = 0, limit = 25, total = items.size))
    }

    private fun viewModel(items: List<Invoice> = emptyList()): BillsViewModel {
        stubPage(items)
        return BillsViewModel(observeSession, getInvoices, getBoleto)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeSession() } returns flowOf(SessionState.LoggedOut)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `bills are always fetched with the paid status filter`() = runTest {
        viewModel = viewModel(listOf(invoice()))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { getInvoices(InvoiceStatus.PAID, 0, any(), true) }
    }

    @Test
    fun `switching the selected contract refetches the paid list`() = runTest {
        val sessionFlow = MutableStateFlow<SessionState>(SessionState.LoggedIn(session(1)))
        every { observeSession() } returns sessionFlow
        viewModel = viewModel(listOf(invoice()))
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { getInvoices(InvoiceStatus.PAID, 0, any(), true) }

        sessionFlow.value = SessionState.LoggedIn(session(2))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { getInvoices(InvoiceStatus.PAID, 0, any(), true) }
    }

    @Test
    fun `openBoleto downloads the pdf and close clears the viewer state`() = runTest {
        val target = invoice(id = 7, boletoPath = "https://demo.local/fatura/7.pdf")
        coEvery { getBoleto(target) } returns AppResult.success(BoletoContent.Pdf(byteArrayOf(37, 80, 68, 70)))
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(7, state.boletoInvoice?.id)
        assertEquals(37.toByte(), (state.boleto as? BoletoContent.Pdf)?.bytes?.get(0))
        assertNull(state.boletoError)
        coVerify(exactly = 1) { getBoleto(target) }

        // Re-tapping the open invoice does not re-download.
        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { getBoleto(any()) }

        viewModel.closeBoleto()
        state = viewModel.uiState.value
        assertNull(state.boletoInvoice)
        assertNull(state.boleto)
    }

    @Test
    fun `boleto download failure keeps the invoice open with an error`() = runTest {
        val target = invoice(id = 7, boletoPath = "https://demo.local/fatura/7.pdf")
        coEvery { getBoleto(target) } returns AppResult.failure(AppError.Timeout)
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.boletoInvoice?.id)
        assertNull(state.boleto)
        assertEquals(AppError.Timeout, state.boletoError)
        coVerify(exactly = 1) { getBoleto(target) }
    }

    @Test
    fun `invoice without a boleto path does nothing`() = runTest {
        viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(invoice(boletoPath = null))

        assertNull(viewModel.uiState.value.boletoInvoice)
        coVerify(exactly = 0) { getBoleto(any()) }
    }
}
