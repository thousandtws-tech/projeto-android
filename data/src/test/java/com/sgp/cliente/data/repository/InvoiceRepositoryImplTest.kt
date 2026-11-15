package com.sgp.cliente.data.repository

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.network.api.BaseUrlProvider
import com.sgp.cliente.data.remote.centralapp.CentralAppRemoteDataSource
import com.sgp.cliente.data.remote.centralapp.CentralInvoiceDto
import com.sgp.cliente.data.remote.centralapp.CentralInvoicePage
import com.sgp.cliente.data.remote.centralapp.InvoiceGroup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class MutableClock(
    private var instant: Instant = Instant.parse("2026-02-20T12:00:00Z"),
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = instant
    fun advance(millis: Long) {
        instant = instant.plusMillis(millis)
    }
}

class InvoiceRepositoryImplTest {

    private val remote = mockk<CentralAppRemoteDataSource>()
    private val clock = MutableClock()
    private val baseUrlProvider = BaseUrlProvider().apply {
        update("https://demo.sgp.net.br")
    }
    private val repository = InvoiceRepositoryImpl(
        remote = remote,
        baseUrlProvider = baseUrlProvider,
        clock = clock,
    )

    private fun page(vararg ids: Int) = CentralInvoicePage(
        items = ids.map {
            CentralInvoiceDto(id = it, status = "Em aberto", dueDate = "10/03/2026", amount = "100.0")
        },
        total = null,
    )

    private fun stubPage(
        group: InvoiceGroup,
        limit: Int = 250,
        result: CentralInvoicePage,
    ) {
        coEvery {
            remote.listInvoices(101, group, "", 0, limit)
        } returns AppResult.success(result)
    }

    @Test
    fun `repeated first-page call within TTL hits cache`() = runTest {
        stubPage(InvoiceGroup.OPEN, result = page(1, 2, 3))

        val first = repository.getInvoices(101, statusFilter = 1)
        val second = repository.getInvoices(101, statusFilter = 1)

        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        assertTrue(second.isSuccess)
        assertEquals(listOf(1, 2, 3), second.getOrNull()!!.items.map { it.id })
    }

    @Test
    fun `different params bypass the cache`() = runTest {
        stubPage(InvoiceGroup.OPEN, result = page(1))
        stubPage(InvoiceGroup.ALL, result = page(2, 3))

        repository.getInvoices(101, statusFilter = 1)
        val other = repository.getInvoices(101, statusFilter = null)

        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.ALL, "", 0, 250) }
        assertEquals(listOf(2, 3), other.getOrNull()!!.items.map { it.id })
    }

    @Test
    fun `forceRefresh bypasses and rewrites the cache`() = runTest {
        coEvery { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) } returnsMany listOf(
            AppResult.success(page(1)),
            AppResult.success(page(9)),
        )

        repository.getInvoices(101, statusFilter = 1)
        val forced = repository.getInvoices(101, statusFilter = 1, forceRefresh = true)
        val cachedAgain = repository.getInvoices(101, statusFilter = 1)

        coVerify(exactly = 2) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        assertEquals(listOf(9), forced.getOrNull()!!.items.map { it.id })
        assertEquals(listOf(9), cachedAgain.getOrNull()!!.items.map { it.id })
    }

    @Test
    fun `expired cache refetches`() = runTest {
        stubPage(InvoiceGroup.OPEN, result = page(1))

        repository.getInvoices(101, statusFilter = 1)
        clock.advance(Duration.ofMinutes(2).toMillis())
        repository.getInvoices(101, statusFilter = 1)

        coVerify(exactly = 2) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
    }

    @Test
    fun `failures are not cached`() = runTest {
        coEvery { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) } returnsMany listOf(
            AppResult.failure(com.sgp.cliente.core.common.error.AppError.Server),
            AppResult.success(page(7)),
        )

        repository.getInvoices(101, statusFilter = 1)
        val retry = repository.getInvoices(101, statusFilter = 1)

        coVerify(exactly = 2) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        assertEquals(listOf(7), retry.getOrNull()!!.items.map { it.id })
    }

    @Test
    fun `concurrent identical calls are coalesced into one request`() = runTest {
        coEvery { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) } coAnswers {
            delay(100)
            AppResult.success(page(1, 2))
        }

        val results = awaitAll(
            async { repository.getInvoices(101, statusFilter = 1) },
            async { repository.getInvoices(101, statusFilter = 1) },
            async { repository.getInvoices(101, statusFilter = 1, forceRefresh = true) },
        )

        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        results.forEach {
            assertEquals(listOf(1, 2), it.getOrNull()!!.items.map { item -> item.id })
        }
    }

    @Test
    fun `different keys are cached independently`() = runTest {
        stubPage(InvoiceGroup.OPEN, result = page(1))
        stubPage(InvoiceGroup.ALL, limit = 25, result = page(5))

        repository.getInvoices(101, statusFilter = 1)
        repository.getInvoices(101, statusFilter = null, limit = 25)
        repository.getInvoices(101, statusFilter = 1)
        repository.getInvoices(101, statusFilter = null, limit = 25)

        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.OPEN, "", 0, 250) }
        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.ALL, "", 0, 25) }
    }

    @Test
    fun `status filter 3 requests the cancelled group`() = runTest {
        stubPage(InvoiceGroup.CANCELLED, result = page(1))

        val result = repository.getInvoices(101, statusFilter = 3)

        coVerify(exactly = 1) { remote.listInvoices(101, InvoiceGroup.CANCELLED, "", 0, 250) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `envelope total is used for PagedResult total`() = runTest {
        coEvery { remote.listInvoices(101, InvoiceGroup.ALL, "", 0, 25) } returns
            AppResult.success(CentralInvoicePage(items = page(1, 2).items, total = 13))

        val result = repository.getInvoices(101, statusFilter = null, limit = 25)

        assertEquals(13, result.getOrNull()!!.total)
    }

    @Test
    fun `bare array page estimates total from page fill`() = runTest {
        coEvery { remote.listInvoices(101, InvoiceGroup.ALL, "", 0, 25) } returns
            AppResult.success(CentralInvoicePage(items = page(1, 2).items, total = null))

        val result = repository.getInvoices(101, statusFilter = null, limit = 25)

        assertEquals(2, result.getOrNull()!!.total)
    }

    @Test
    fun `relative boleto link resolves against the configured base url`() = runTest {
        stubPage(
            InvoiceGroup.OPEN,
            result = CentralInvoicePage(
                items = listOf(
                    CentralInvoiceDto(
                        id = 7,
                        status = "Em aberto",
                        dueDate = "10/03/2026",
                        amount = "100.0",
                        boletoUrl = "/fatura/boleto/abc123.pdf",
                    ),
                ),
                total = null,
            ),
        )

        val result = repository.getInvoices(101, statusFilter = 1)

        assertEquals(
            "https://demo.sgp.net.br/fatura/boleto/abc123.pdf",
            result.getOrNull()!!.items.single().boletoPath,
        )
    }

    @Test
    fun `absolute boleto link passes through unchanged`() = runTest {
        stubPage(
            InvoiceGroup.OPEN,
            result = CentralInvoicePage(
                items = listOf(
                    CentralInvoiceDto(
                        id = 8,
                        status = "Em aberto",
                        dueDate = "10/03/2026",
                        amount = "100.0",
                        chargeUrl = "https://pgw.exemplo.com/c/xyz",
                    ),
                ),
                total = null,
            ),
        )

        val result = repository.getInvoices(101, statusFilter = 1)

        assertEquals(
            "https://pgw.exemplo.com/c/xyz",
            result.getOrNull()!!.items.single().boletoPath,
        )
    }

    @Test
    fun `group api values match the live server contract`() {
        // Verified against demo.sgp.net.br: feminine plurals; "todos" is a
        // recognized-but-dead filter that always returns an empty page.
        assertEquals("abertos", InvoiceGroup.OPEN.apiValue)
        assertEquals("pagos", InvoiceGroup.PAID.apiValue)
        assertEquals("canceladas", InvoiceGroup.CANCELLED.apiValue)
        assertEquals("todas", InvoiceGroup.ALL.apiValue)
    }
}
