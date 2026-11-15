package com.sgp.cliente.feature.billdetails

import androidx.lifecycle.SavedStateHandle
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.usecase.invoice.GetInvoiceDetailsUseCase
import com.sgp.cliente.domain.usecase.invoice.SendInvoiceCopyUseCase
import com.sgp.cliente.domain.usecase.payment.GeneratePixUseCase
import com.sgp.cliente.domain.usecase.payment.RequestPaymentPromiseUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getDetails: GetInvoiceDetailsUseCase = mockk()
    private val generatePix: GeneratePixUseCase = mockk()
    private val sendCopy: SendInvoiceCopyUseCase = mockk()
    private val requestPaymentPromise: RequestPaymentPromiseUseCase = mockk()
    private lateinit var viewModel: BillDetailsViewModel

    private val promise = PaymentPromise(
        status = 1,
        protocol = "P-2026-01",
        released = true,
        message = "Promessa registrada",
    )

    private fun invoice(
        id: Int = 42,
        barcode: String? = "34191.79001 01043.510047 91020.150008 9 96770000010000",
        pixCode: String? = null,
        canGeneratePix: Boolean = false,
    ) = Invoice(
        id = id,
        documentNumber = null,
        statusId = 1,
        statusLabel = "Em aberto",
        dueDate = LocalDate.of(2026, 3, 10),
        effectiveDueDate = null,
        amount = 100.0,
        correctedAmount = null,
        paidAt = null,
        barcode = barcode,
        pixCode = pixCode,
        canGeneratePix = canGeneratePix,
        boletoPath = null,
    )

    private fun viewModel(invoiceId: Int? = 42): BillDetailsViewModel {
        val handle = if (invoiceId == null) {
            SavedStateHandle()
        } else {
            SavedStateHandle(mapOf("invoiceId" to invoiceId))
        }
        return BillDetailsViewModel(handle, getDetails, generatePix, sendCopy, requestPaymentPromise)
    }

    private fun stubLoaded(invoice: Invoice) {
        coEvery { getDetails(invoice.id) } returns AppResult.success(invoice)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the invoice referenced by the invoiceId navigation argument`() = runTest {
        stubLoaded(invoice(id = 42))

        viewModel = viewModel(invoiceId = 42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(42, state.invoice?.id)
        coVerify(exactly = 1) { getDetails(42) }
    }

    @Test
    fun `load failure surfaces the typed error`() = runTest {
        coEvery { getDetails(42) } returns AppResult.failure(AppError.NotFound)

        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.invoice)
        assertEquals(AppError.NotFound, state.error)
    }

    @Test(expected = IllegalStateException::class)
    fun `missing invoiceId argument fails fast`() {
        viewModel(invoiceId = null)
    }

    @Test
    fun `invoice carrying an inline PIX code exposes it without generating`() = runTest {
        stubLoaded(invoice(pixCode = "00020126-pix-copia-e-cola"))

        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("00020126-pix-copia-e-cola", state.pixCode)
        assertFalse(state.canRequestPix)

        viewModel.loadPix()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { generatePix(any()) }
    }

    @Test
    fun `loadPix generates the PIX code for invoices that allow it`() = runTest {
        stubLoaded(invoice(canGeneratePix = true))
        coEvery { generatePix(42) } returns
            AppResult.success(PixCharge(status = 1, message = "ok", code = "pix-gerado"))
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canRequestPix)

        viewModel.loadPix()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loadingPix)
        assertNull(state.actionError)
        assertEquals("pix-gerado", state.generatedPix)
        assertEquals("pix-gerado", state.pixCode)
        assertFalse(state.canRequestPix)
        coVerify(exactly = 1) { generatePix(42) }
    }

    @Test
    fun `PIX business failure surfaces the API message as action error`() = runTest {
        stubLoaded(invoice(canGeneratePix = true))
        coEvery { generatePix(42) } returns
            AppResult.success(PixCharge(status = 0, message = "PIX indisponível", code = null))
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadPix()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loadingPix)
        assertNull(state.generatedPix)
        assertNull(state.pixCode)
        assertEquals(AppError.Api("PIX indisponível"), state.actionError)
    }

    @Test
    fun `PIX transport failure surfaces the typed error`() = runTest {
        stubLoaded(invoice(canGeneratePix = true))
        coEvery { generatePix(42) } returns AppResult.failure(AppError.Server)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadPix()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loadingPix)
        assertEquals(AppError.Server, state.actionError)
    }

    @Test
    fun `sendCopyByEmail requests the email channel and exposes the confirmation`() = runTest {
        stubLoaded(invoice())
        coEvery { sendCopy(42, SendChannel.EMAIL, null) } returns
            AppResult.success("Cópia enviada por e-mail")
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.sendCopyByEmail()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sendingCopy)
        assertNull(state.actionError)
        assertEquals("Cópia enviada por e-mail", state.sentCopyMessage)
        coVerify(exactly = 1) { sendCopy(42, SendChannel.EMAIL, null) }
    }

    @Test
    fun `sendCopy failure surfaces the action error`() = runTest {
        stubLoaded(invoice())
        coEvery { sendCopy(42, SendChannel.EMAIL, null) } returns
            AppResult.failure(AppError.Timeout)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.sendCopyByEmail()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sendingCopy)
        assertNull(state.sentCopyMessage)
        assertEquals(AppError.Timeout, state.actionError)
    }

    @Test
    fun `requestPromise success exposes the promise`() = runTest {
        stubLoaded(invoice())
        coEvery { requestPaymentPromise() } returns AppResult.success(promise)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPromise()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.requestingPromise)
        assertNull(state.actionError)
        assertEquals(promise, state.promise)
    }

    @Test
    fun `requestPromise failure surfaces the action error`() = runTest {
        stubLoaded(invoice())
        coEvery { requestPaymentPromise() } returns AppResult.failure(AppError.Unauthorized)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.requestPromise()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.requestingPromise)
        assertNull(state.promise)
        assertEquals(AppError.Unauthorized, state.actionError)
    }

    @Test
    fun `consume functions clear the one-shot fields`() = runTest {
        stubLoaded(invoice())
        coEvery { sendCopy(42, SendChannel.EMAIL, null) } returns AppResult.success("ok")
        coEvery { requestPaymentPromise() } returns AppResult.success(promise)
        coEvery { generatePix(42) } returns AppResult.failure(AppError.Server)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.sendCopyByEmail()
        viewModel.requestPromise()
        viewModel.loadPix()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.consumeSentCopyMessage()
        viewModel.consumePromise()
        viewModel.consumeActionError()

        val state = viewModel.uiState.value
        assertNull(state.sentCopyMessage)
        assertNull(state.promise)
        assertNull(state.actionError)
    }
}
