package com.sgp.cliente.feature.paymentpromises

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.invoice.GetOpenInvoicesUseCase
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import com.sgp.cliente.domain.usecase.payment.RequestPaymentPromiseUseCase
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
class PaymentPromisesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeSession: ObserveSessionUseCase = mockk()
    private val getOpenInvoices: GetOpenInvoicesUseCase = mockk()
    private val requestPromise: RequestPaymentPromiseUseCase = mockk()
    private lateinit var viewModel: PaymentPromisesViewModel

    private val open = OpenInvoices(
        overdue = listOf(invoice(1)),
        upcoming = listOf(invoice(2)),
    )
    private val promise = PaymentPromise(
        status = 1,
        protocol = "P-2026-01",
        released = true,
        message = "Promessa registrada",
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
        every { observeSession() } returns flowOf(SessionState.LoggedOut)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful load exposes the open invoices`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)

        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(listOf(1, 2), state.openInvoices.all.map { it.id })
    }

    @Test
    fun `load failure surfaces the typed error`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.failure(AppError.Server)

        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(AppError.Server, state.error)
        assertTrue(state.openInvoices.all.isEmpty())
    }

    @Test
    fun `reload after a failure clears the error`() = runTest {
        coEvery { getOpenInvoices(any()) } returnsMany listOf(
            AppResult.failure(AppError.Server),
            AppResult.success(open),
        )
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppError.Server, viewModel.uiState.value.error)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.loading)
        assertEquals(listOf(1, 2), state.openInvoices.all.map { it.id })
    }

    @Test
    fun `successful request exposes the promise result`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { requestPromise() } returns AppResult.success(promise)
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.request()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.requesting)
        assertNull(state.actionError)
        assertEquals(promise, state.result)
        assertTrue(state.result!!.released)
        coVerify(exactly = 1) { requestPromise() }
    }

    @Test
    fun `request flags requesting while the call is in flight`() = runTest {
        val gate = CompletableDeferred<AppResult<PaymentPromise>>()
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { requestPromise() } coAnswers { gate.await() }
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.request()
        dispatcher.scheduler.runCurrent()

        // The screen drives the action button's loading state from this flag.
        assertTrue(viewModel.uiState.value.requesting)
        assertNull(viewModel.uiState.value.actionError)

        gate.complete(AppResult.success(promise))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.requesting)
        assertEquals(promise, viewModel.uiState.value.result)
    }

    @Test
    fun `request failure surfaces the action error without touching the loaded list`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { requestPromise() } returns AppResult.failure(AppError.Server)
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.request()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.requesting)
        assertNull(state.result)
        assertEquals(AppError.Server, state.actionError)
        assertNull(state.error)
        assertEquals(listOf(1, 2), state.openInvoices.all.map { it.id })
    }

    @Test
    fun `consumeResult clears the promise dialog state`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { requestPromise() } returns AppResult.success(promise)
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.request()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(promise, viewModel.uiState.value.result)

        viewModel.consumeResult()

        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `consumeActionError clears the snackbar state`() = runTest {
        coEvery { getOpenInvoices(any()) } returns AppResult.success(open)
        coEvery { requestPromise() } returns AppResult.failure(AppError.Server)
        viewModel = PaymentPromisesViewModel(observeSession, getOpenInvoices, requestPromise)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.request()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppError.Server, viewModel.uiState.value.actionError)

        viewModel.consumeActionError()

        assertNull(viewModel.uiState.value.actionError)
    }
}
