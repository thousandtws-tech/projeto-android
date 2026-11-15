package com.sgp.cliente.feature.bills

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.usecase.invoice.GetBoletoPdfUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoicesUseCase
import com.sgp.cliente.domain.usecase.payment.GeneratePixUseCase
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
    private val generatePix: GeneratePixUseCase = mockk()
    private val getBoletoPdf: GetBoletoPdfUseCase = mockk()
    private lateinit var viewModel: BillsViewModel

    private fun invoice(
        id: Int = 42,
        pixCode: String? = null,
        canGeneratePix: Boolean = false,
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
        pixCode = pixCode,
        canGeneratePix = canGeneratePix,
        boletoPath = boletoPath,
    )

    private fun stubPage(items: List<Invoice>) {
        coEvery { getInvoices(any(), any(), any(), any()) } returns
            AppResult.success(PagedResult(items = items, offset = 0, limit = 25, total = items.size))
    }

    private fun viewModel(items: List<Invoice> = emptyList()): BillsViewModel {
        stubPage(items)
        return BillsViewModel(getInvoices, generatePix, getBoletoPdf)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `inline pix code is handed to the clipboard without a network call`() = runTest {
        viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPixCode(invoice(pixCode = "00020126-inline"))

        assertEquals("00020126-inline", viewModel.uiState.value.pixCodeToCopy)
        coVerify(exactly = 0) { generatePix(any()) }
    }

    @Test
    fun `pix is generated on demand, queued for copy and kept on the invoice`() = runTest {
        val target = invoice(id = 9, canGeneratePix = true)
        coEvery { generatePix(9) } returns
            AppResult.success(PixCharge(status = 1, message = "ok", code = "pix-gerado"))
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPixCode(target)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("pix-gerado", state.pixCodeToCopy)
        assertNull(state.generatingPixId)
        assertNull(state.actionError)
        // The generated code is kept on the invoice — next tap copies directly.
        assertEquals("pix-gerado", state.invoices.single { it.id == 9 }.pixCode)
        coVerify(exactly = 1) { generatePix(9) }

        viewModel.consumePixCodeToCopy()
        viewModel.requestPixCode(state.invoices.single { it.id == 9 })
        assertEquals("pix-gerado", viewModel.uiState.value.pixCodeToCopy)
        coVerify(exactly = 1) { generatePix(any()) }
    }

    @Test
    fun `pix business failure surfaces the API message and clears the spinner`() = runTest {
        val target = invoice(id = 9, canGeneratePix = true)
        coEvery { generatePix(9) } returns
            AppResult.success(PixCharge(status = 0, message = "PIX indisponível", code = null))
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPixCode(target)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pixCodeToCopy)
        assertNull(state.generatingPixId)
        assertEquals(AppError.Api("PIX indisponível"), state.actionError)
    }

    @Test
    fun `pix transport failure surfaces the typed error`() = runTest {
        val target = invoice(id = 9, canGeneratePix = true)
        coEvery { generatePix(9) } returns AppResult.failure(AppError.Timeout)
        viewModel = viewModel(listOf(target))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPixCode(target)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pixCodeToCopy)
        assertNull(state.generatingPixId)
        assertEquals(AppError.Timeout, state.actionError)
    }

    @Test
    fun `invoice without code and without generation capability is a no-op`() = runTest {
        viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPixCode(invoice(pixCode = null, canGeneratePix = false))

        val state = viewModel.uiState.value
        assertNull(state.pixCodeToCopy)
        assertNull(state.generatingPixId)
        coVerify(exactly = 0) { generatePix(any()) }
    }

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
