package com.sgp.cliente.feature.billdetails

import androidx.lifecycle.SavedStateHandle
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.usecase.invoice.GetBoletoPdfUseCase
import com.sgp.cliente.domain.usecase.invoice.GetInvoiceDetailsUseCase
import com.sgp.cliente.domain.usecase.invoice.SendInvoiceCopyUseCase
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
    private val getBoletoPdf: GetBoletoPdfUseCase = mockk()
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
        boletoPath: String? = null,
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
        pixCode = null,
        canGeneratePix = false,
        boletoPath = boletoPath,
    )

    private fun viewModel(invoiceId: Int? = 42): BillDetailsViewModel {
        val handle = if (invoiceId == null) {
            SavedStateHandle()
        } else {
            SavedStateHandle(mapOf("invoiceId" to invoiceId))
        }
        return BillDetailsViewModel(handle, getDetails, getBoletoPdf, sendCopy, requestPaymentPromise)
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

    @Test
    fun `missing invoiceId stays idle until show is called`() = runTest {
        viewModel = viewModel(invoiceId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loading)
        coVerify(exactly = 0) { getDetails(any()) }

        stubLoaded(invoice(id = 42))
        viewModel.show(42)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(42, viewModel.uiState.value.invoice?.id)
        coVerify(exactly = 1) { getDetails(42) }
    }

    @Test
    fun `show resets state and loads a different invoice`() = runTest {
        stubLoaded(invoice(id = 42))
        coEvery { getDetails(7) } returns AppResult.success(invoice(id = 7))
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.show(7)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.invoice?.id)
        assertFalse(state.boletoOpen)
        assertNull(state.sentCopyMessage)
        coVerify(exactly = 1) { getDetails(7) }
    }

    @Test
    fun `openBoleto downloads the pdf and close clears the viewer`() = runTest {
        val target = invoice(boletoPath = "https://demo.local/fatura/42.pdf")
        stubLoaded(target)
        coEvery { getBoletoPdf(target) } returns AppResult.success(byteArrayOf(37, 80, 68, 70))
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto()
        dispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.boletoOpen)
        assertFalse(state.boletoLoading)
        assertEquals(37.toByte(), state.boletoPdf?.get(0))
        coVerify(exactly = 1) { getBoletoPdf(target) }

        viewModel.closeBoleto()
        state = viewModel.uiState.value
        assertFalse(state.boletoOpen)
        assertNull(state.boletoPdf)
    }

    @Test
    fun `boleto download failure keeps the viewer open with an error`() = runTest {
        val target = invoice(boletoPath = "https://demo.local/fatura/42.pdf")
        stubLoaded(target)
        coEvery { getBoletoPdf(target) } returns AppResult.failure(AppError.Timeout)
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.boletoOpen)
        assertNull(state.boletoPdf)
        assertEquals(AppError.Timeout, state.boletoError)
    }

    @Test
    fun `invoice without a boleto path does not open the viewer`() = runTest {
        stubLoaded(invoice(boletoPath = null))
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBoleto()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.boletoOpen)
        coVerify(exactly = 0) { getBoletoPdf(any()) }
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
        viewModel = viewModel(42)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.sendCopyByEmail()
        viewModel.requestPromise()
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
