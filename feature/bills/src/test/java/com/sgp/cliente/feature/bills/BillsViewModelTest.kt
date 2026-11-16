package com.sgp.cliente.feature.bills

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.usecase.invoice.GetBoletoPdfUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoicesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val getInvoices: GetInvoicesUseCase = mockk()
    private val getBoletoPdf: GetBoletoPdfUseCase = mockk()
    private lateinit var viewModel: BillsViewModel

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
        return BillsViewModel(getInvoices, getBoletoPdf)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `openBoleto downloads the pdf and close clears the viewer state`() = runTest {
        val target = invoice(id = 7, boletoPath = "https://demo.local/fatura/7.pdf")
        coEvery { getBoletoPdf(target) } returns AppResult.success(byteArrayOf(37, 80, 68, 70))
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(7, state.boletoInvoice?.id)
        assertEquals(37.toByte(), state.boletoPdf?.get(0))
        assertNull(state.boletoError)
        coVerify(exactly = 1) { getBoletoPdf(target) }

        // Re-tapping the open invoice does not re-download.
        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { getBoletoPdf(any()) }

        viewModel.closeBoleto()
        state = viewModel.uiState.value
        assertNull(state.boletoInvoice)
        assertNull(state.boletoPdf)
    }

    @Test
    fun `boleto download failure keeps the invoice open with an error`() = runTest {
        val target = invoice(id = 7, boletoPath = "https://demo.local/fatura/7.pdf")
        coEvery { getBoletoPdf(target) } returns AppResult.failure(AppError.Timeout)
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(target)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.boletoInvoice?.id)
        assertNull(state.boletoPdf)
        assertEquals(AppError.Timeout, state.boletoError)
        coVerify(exactly = 1) { getBoletoPdf(target) }
    }

    @Test
    fun `invoice without a boleto path does nothing`() = runTest {
        viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto(invoice(boletoPath = null))

        assertNull(viewModel.uiState.value.boletoInvoice)
        coVerify(exactly = 0) { getBoletoPdf(any()) }
    }
}
