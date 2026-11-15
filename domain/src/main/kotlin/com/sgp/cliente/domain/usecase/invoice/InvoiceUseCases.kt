package com.sgp.cliente.domain.usecase.invoice

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.map
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.SecondCopy
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.repository.InvoiceRepository
import com.sgp.cliente.domain.repository.SessionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Open bills partitioned for the UI: overdue first, then bills not yet due.
 * The API is the source of truth — this class only partitions what it returns.
 */
data class OpenInvoices(
    val overdue: List<Invoice>,
    val upcoming: List<Invoice>,
) {
    val all: List<Invoice> get() = overdue + upcoming
}

class GetOpenInvoicesUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): AppResult<OpenInvoices> {
        val contractId = contractIdOrNull()
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        val today = LocalDate.now(clock)
        return invoiceRepository.getInvoices(
            contractId = contractId,
            statusFilter = InvoiceStatus.OPEN.apiId,
            forceRefresh = forceRefresh,
        ).map { page ->
            val (overdue, upcoming) = page.items
                .distinctBy(Invoice::id)
                .partition { it.isOverdue(today) }
            OpenInvoices(
                overdue = overdue.sortedBy { it.effectiveOrOriginalDueDate },
                upcoming = upcoming.sortedBy { it.effectiveOrOriginalDueDate },
            )
        }
    }

    private suspend fun contractIdOrNull(): Int? =
        sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
}

/**
 * Paged invoice listing (`POST /api/centralapp/titulo/list/`).
 * [statusFilter] is applied client-side over the `todas` stream — the
 * server's `grupo` values cannot express "all open" or "cancelled".
 */
class GetInvoicesUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        statusFilter: InvoiceStatus? = null,
        offset: Int = 0,
        limit: Int = 25,
        forceRefresh: Boolean = false,
    ): AppResult<PagedResult<Invoice>> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return invoiceRepository.getInvoices(
            contractId = contractId,
            statusFilter = statusFilter?.apiId,
            offset = offset,
            limit = limit,
            forceRefresh = forceRefresh,
        )
    }
}

class GetPaidInvoicesUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        offset: Int = 0,
        limit: Int = 250,
    ): AppResult<PagedResult<Invoice>> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return invoiceRepository.getInvoices(
            contractId = contractId,
            statusFilter = InvoiceStatus.PAID.apiId,
            offset = offset,
            limit = limit,
        )
    }
}

class GetInvoiceDetailsUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(invoiceId: Int): AppResult<Invoice> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return invoiceRepository.getInvoiceById(contractId, invoiceId)
    }
}

class GetSecondCopyUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): AppResult<SecondCopy> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return invoiceRepository.getSecondCopy(contractId)
    }
}

/**
 * Downloads the invoice boleto PDF for the in-app viewer. Providers may
 * embed the PIX QR inside the boleto, so this is also the PIX path for
 * invoices without `codigo_pix`/`pix_sob_demanda`.
 */
class GetBoletoPdfUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
) {
    suspend operator fun invoke(invoice: Invoice): AppResult<ByteArray> {
        val url = invoice.boletoPath
            ?: return AppResult.failure(AppError.Validation("Fatura sem boleto disponível"))
        return invoiceRepository.getBoletoPdf(url)
    }
}

class SendInvoiceCopyUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        invoiceId: Int,
        channel: SendChannel,
        destination: String? = null,
    ): AppResult<String> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return invoiceRepository.sendCopy(contractId, invoiceId, channel, destination)
    }
}
