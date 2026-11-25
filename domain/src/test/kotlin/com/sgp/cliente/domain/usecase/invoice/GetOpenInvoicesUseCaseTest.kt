package com.sgp.cliente.domain.usecase.invoice

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.SecondCopy
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.InvoiceRepository
import com.sgp.cliente.domain.repository.SessionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSessionRepository(
    private val contractId: Int? = 7,
) : SessionRepository {
    override val state: Flow<SessionState> = MutableStateFlow(SessionState.LoggedOut)
    override suspend fun persist(session: CustomerSession) = Unit
    override suspend fun updateContracts(contracts: List<Contract>) = Unit
    override suspend fun selectContract(contractId: Int) = Unit
    override suspend fun credentials(): Credentials? = null
    override suspend fun selectedContractId(): Int? = contractId
    override suspend fun selectedContract(): Contract? = null
    override suspend fun authToken(): String? = null
    override suspend fun persistAuthToken(token: String) = Unit
    override suspend fun updatePassword(newPassword: String) = Unit
    override suspend fun clear() = Unit
}

private class FakeInvoiceRepository(
    var invoices: List<Invoice> = emptyList(),
    var result: AppResult<PagedResult<Invoice>>? = null,
) : InvoiceRepository {
    var lastStatusFilter: Int? = null
    var lastContractId: Int? = null
    var lastForceRefresh: Boolean? = null

    override suspend fun getInvoices(
        contractId: Int,
        statusFilter: Int?,
        offset: Int,
        limit: Int,
        forceRefresh: Boolean,
    ): AppResult<PagedResult<Invoice>> {
        lastContractId = contractId
        lastStatusFilter = statusFilter
        lastForceRefresh = forceRefresh
        return result ?: AppResult.success(
            PagedResult(items = invoices, offset = 0, limit = limit, total = invoices.size),
        )
    }

    override suspend fun getSecondCopy(contractId: Int): AppResult<SecondCopy> =
        AppResult.failure(AppError.Api("not implemented"))

    override suspend fun sendCopy(
        contractId: Int,
        invoiceId: Int,
        channel: SendChannel,
        destination: String?,
    ): AppResult<String> = AppResult.success("ok")

    override suspend fun getInvoiceById(contractId: Int, invoiceId: Int): AppResult<Invoice> =
        invoices.firstOrNull { it.id == invoiceId }
            ?.let { AppResult.success(it) }
            ?: AppResult.failure(AppError.NotFound)

    override suspend fun getBoleto(url: String): AppResult<BoletoContent> =
        AppResult.failure(AppError.Api("not implemented"))
}

class GetOpenInvoicesUseCaseTest {

    private val fixedToday = LocalDate.of(2026, 2, 20)
    private val clock = Clock.fixed(
        Instant.parse("2026-02-20T12:00:00Z"),
        ZoneId.of("America/Sao_Paulo"),
    )

    private lateinit var invoiceRepository: FakeInvoiceRepository

    @Before
    fun setUp() {
        invoiceRepository = FakeInvoiceRepository()
    }

    private fun invoice(id: Int, dueDate: String) = Invoice(
        id = id,
        documentNumber = null,
        statusId = 1,
        statusLabel = "Em aberto",
        dueDate = LocalDate.parse(dueDate),
        effectiveDueDate = null,
        amount = 100.0,
        correctedAmount = null,
        paidAt = null,
        barcode = null,
        pixCode = null,
        canGeneratePix = false,
        boletoPath = null,
    )

    @Test
    fun `overdue invoices come before upcoming ones`() = runTest {
        invoiceRepository.invoices = listOf(
            invoice(1, "2026-03-10"),
            invoice(2, "2026-01-15"),
            invoice(3, "2026-02-10"),
        )
        val useCase = GetOpenInvoicesUseCase(
            invoiceRepository = invoiceRepository,
            sessionRepository = FakeSessionRepository(),
            clock = clock,
        )
        val result = useCase()
        assertTrue(result.isSuccess)
        val open = result.getOrNull()!!
        assertEquals(listOf(2, 3), open.overdue.map { it.id })
        assertEquals(listOf(1), open.upcoming.map { it.id })
        assertEquals(3, open.all.size)
    }

    @Test
    fun `request uses open status filter and selected contract`() = runTest {
        val useCase = GetOpenInvoicesUseCase(
            invoiceRepository = invoiceRepository,
            sessionRepository = FakeSessionRepository(contractId = 42),
            clock = clock,
        )
        useCase()
        assertEquals(42, invoiceRepository.lastContractId)
        assertEquals(1, invoiceRepository.lastStatusFilter)
    }

    @Test
    fun `fails fast when no contract is selected`() = runTest {
        val useCase = GetOpenInvoicesUseCase(
            invoiceRepository = invoiceRepository,
            sessionRepository = FakeSessionRepository(contractId = null),
            clock = clock,
        )
        val result = useCase()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is AppError.Validation)
    }

    @Test
    fun `forwards forceRefresh flag to repository`() = runTest {
        val useCase = GetOpenInvoicesUseCase(
            invoiceRepository = invoiceRepository,
            sessionRepository = FakeSessionRepository(),
            clock = clock,
        )
        useCase(forceRefresh = true)
        assertEquals(true, invoiceRepository.lastForceRefresh)
    }

    @Test
    fun `duplicate ids are removed`() = runTest {
        invoiceRepository.invoices = listOf(
            invoice(1, "2026-03-10"),
            invoice(1, "2026-03-10"),
        )
        val useCase = GetOpenInvoicesUseCase(
            invoiceRepository = invoiceRepository,
            sessionRepository = FakeSessionRepository(),
            clock = clock,
        )
        assertEquals(1, useCase().getOrNull()!!.all.size)
    }
}
