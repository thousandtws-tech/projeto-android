package com.sgp.cliente.data.repository

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.common.result.map
import com.sgp.cliente.core.network.api.BaseUrlProvider
import com.sgp.cliente.data.mapper.toDomain
import com.sgp.cliente.data.mapper.toMessageOrError
import com.sgp.cliente.data.mapper.toPaymentPromise
import com.sgp.cliente.data.mapper.toSecondCopy
import com.sgp.cliente.data.remote.centralapp.CentralAppRemoteDataSource
import com.sgp.cliente.data.remote.centralapp.CentralEntryDto
import com.sgp.cliente.data.remote.centralapp.InvoiceGroup
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.model.CentralOption
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.model.SecondCopy
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.AuthRepository
import com.sgp.cliente.domain.repository.ContractRepository
import com.sgp.cliente.domain.repository.InvoiceRepository
import com.sgp.cliente.domain.repository.PaymentRepository
import com.sgp.cliente.domain.repository.SessionRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: CentralAppRemoteDataSource,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val baseUrlProvider: BaseUrlProvider,
) : AuthRepository {

    override suspend fun login(login: String, password: String): AppResult<CustomerSession> =
        remote.login(login, password).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                val token = dto.jwtToken
                if (token.isNullOrBlank()) {
                    return@fold AppResult.failure(
                        AppError.Api(dto.message ?: "Credenciais inválidas"),
                    )
                }
                // The contract list (and everything else) rides on the JWT.
                sessionRepository.persistAuthToken(token)

                remote.listContracts().fold(
                    onFailure = { AppResult.failure(it) },
                    onSuccess = { contractDtos ->
                        val contracts = contractDtos.map { it.toDomain() }
                        AppResult.success(
                            CustomerSession(
                                credentials = Credentials(login, password),
                                customerName = dto.customerName
                                    ?: dto.user?.name
                                    ?: contracts.firstOrNull()?.razaoSocial.orEmpty(),
                                cpfCnpj = dto.cpfCnpj
                                    ?: contracts.firstOrNull()?.cpfCnpj.orEmpty(),
                                contracts = contracts,
                                selectedContractId = dto.contractId?.toIntOrNull()
                                    ?: contracts.firstOrNull { it.isActive }?.id
                                    ?: contracts.firstOrNull()?.id,
                            ),
                        )
                    },
                )
            },
        )

    override suspend fun discoverAndSelectCentral(baseUrl: String): AppResult<CentralOption> {
        baseUrlProvider.update(baseUrl)
        return remote.listCentrals().fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { centrals ->
                val pick = centrals.preferredCentral()
                if (pick == null) {
                    AppResult.failure(
                        AppError.Api("Nenhuma Central do Assinante encontrada neste servidor"),
                    )
                } else {
                    settingsRepository.setCentral(pick.id, pick.token)
                    AppResult.success(
                        CentralOption(id = pick.id, name = pick.name.orEmpty()),
                    )
                }
            },
        )
    }

    /** "Central do Assinante" first, then any enabled entry. */
    private fun List<CentralEntryDto>.preferredCentral(): CentralEntryDto? =
        firstOrNull { it.name.equals("Central do Assinante", ignoreCase = true) }
            ?: firstOrNull { !it.token.isNullOrBlank() }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): AppResult<Unit> {
        val session = (sessionRepository.state.first() as? SessionState.LoggedIn)?.session
            ?: return AppResult.failure(AppError.Unauthorized)
        return remote.changePassword(
            customerName = session.customerName,
            currentPassword = currentPassword,
            newPassword = newPassword,
        ).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                if (dto.status == STATUS_OK) {
                    // The API issues a fresh JWT; the stored password must be
                    // re-ciphered too or the next silent re-login would fail.
                    dto.jwtToken?.let { sessionRepository.persistAuthToken(it) }
                    sessionRepository.updatePassword(newPassword)
                    AppResult.success(Unit)
                } else {
                    AppResult.failure(
                        AppError.Api(dto.message ?: "Não foi possível alterar a senha"),
                    )
                }
            },
        )
    }

    private companion object {
        const val STATUS_OK = 1
    }
}

@Singleton
class ContractRepositoryImpl @Inject constructor(
    private val remote: CentralAppRemoteDataSource,
) : ContractRepository {

    override suspend fun refreshContracts(): AppResult<List<Contract>> =
        remote.listContracts().map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun checkServiceStatus(contractId: Int): AppResult<ServiceStatus> =
        remote.serviceStatus(contractId).map { it.toDomain() }
}

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val remote: CentralAppRemoteDataSource,
    private val baseUrlProvider: BaseUrlProvider,
    private val clock: Clock = Clock.systemDefaultZone(),
) : InvoiceRepository {

    /** In-memory page cache — powers detail lookups without refetching. */
    @Volatile
    private var lastPage: Pair<Int, List<Invoice>>? = null

    private data class PageKey(
        val contractId: Int,
        val statusFilter: Int?,
        val offset: Int,
        val limit: Int,
    )

    private data class PageCacheEntry(
        val page: PagedResult<Invoice>,
        val atMillis: Long,
    )

    /**
     * Short-TTL cache of recently fetched pages, keyed by request params.
     * Screens that ask for the same slice (e.g. dashboard and payment-promises,
     * both loading open invoices) reuse it instead of hitting the network again.
     */
    @Volatile
    private var pageCache: Map<PageKey, PageCacheEntry> = emptyMap()

    /**
     * Per-key mutexes coalesce concurrent identical requests: a caller that
     * arrives while a fetch for the same key is in flight waits for it and
     * reuses its just-fetched result instead of duplicating the network call.
     */
    private val keyLocksMutex = Mutex()
    private val keyLocks = mutableMapOf<PageKey, Mutex>()

    override suspend fun getInvoices(
        contractId: Int,
        statusFilter: Int?,
        offset: Int,
        limit: Int,
        forceRefresh: Boolean,
    ): AppResult<PagedResult<Invoice>> {
        // Server `grupo` values cannot express our filters — `abertos` is
        // only the payable-now subset, `canceladas` is ignored and falls
        // back to everything — so we always page the raw `todas` stream and
        // filter client-side. The cache key skips the filter on purpose:
        // all tabs share the same raw pages.
        val key = PageKey(contractId, null, offset, limit)
        if (!forceRefresh) {
            cachedPage(key)?.let { return AppResult.success(it.filteredBy(contractId, statusFilter)) }
        }
        val lock = keyLocksMutex.withLock {
            if (keyLocks.size >= MAX_KEY_LOCKS) keyLocks.clear()
            keyLocks.getOrPut(key) { Mutex() }
        }
        if (lock.tryLock()) {
            lock.unlock()
        } else {
            // Another caller is fetching this key right now — join it. The
            // result it is about to store is as fresh as a new request.
            val arrivedAt = clock.millis()
            lock.lock()
            lock.unlock()
            pageCache[key]
                ?.takeIf { it.atMillis >= arrivedAt }
                ?.let { return AppResult.success(it.page.filteredBy(contractId, statusFilter)) }
            // The in-flight fetch failed — fall through and retry ourselves.
        }
        return lock.withLock {
            if (!forceRefresh) {
                cachedPage(key)?.let { return@withLock AppResult.success(it.filteredBy(contractId, statusFilter)) }
            }
            remote.listInvoices(
                contractId = contractId,
                group = InvoiceGroup.ALL,
                search = "",
                offset = offset,
                limit = limit,
            ).map { dtoPage ->
                val raw = dtoPage.items.mapNotNull { it.toDomain()?.withResolvedBoleto() }
                val page = PagedResult(
                    items = raw,
                    offset = offset,
                    limit = limit,
                    // The {"total",…,"result"} envelope carries the real
                    // count; a bare array means we estimate from page fill.
                    total = dtoPage.total ?: if (raw.size >= limit) {
                        offset + raw.size + 1
                    } else {
                        offset + raw.size
                    },
                    consumed = raw.size,
                )
                if (offset == 0) lastPage = contractId to raw
                storePage(key, page)
                page.filteredBy(contractId, statusFilter)
            }
        }
    }

    /** Client-side status filter over a raw stream page — keeps the raw
     * [PagedResult.consumed] so `hasMore`/`nextOffset` still walk the
     * unfiltered stream. */
    private fun PagedResult<Invoice>.filteredBy(
        contractId: Int,
        statusFilter: Int?,
    ): PagedResult<Invoice> =
        if (statusFilter == null) {
            this
        } else {
            var filtered = items.filter { it.statusId == statusFilter }
            if (statusFilter == InvoiceStatus.OPEN.apiId) {
                // Provider rule: only the currently-payable boleto shows —
                // the earliest open due date (overdue wins); later
                // competences appear once this one is settled. An open
                // invoice without a due date stays visible (fail-open).
                val firstDue = earliestOpenDue(contractId, filtered)
                filtered = filtered.filter {
                    it.effectiveOrOriginalDueDate.let { due -> due == null || due == firstDue }
                }
            }
            copy(items = filtered)
        }

    /** Earliest open due date for [contractId] across page 0 plus the
     * current page — keeps the payable-competence rule stable when the
     * same month is split across page boundaries. */
    private fun earliestOpenDue(contractId: Int, currentPage: List<Invoice>): LocalDate? {
        val firstPage = lastPage?.takeIf { it.first == contractId }?.second.orEmpty()
        return (firstPage + currentPage)
            .filter { it.statusId == InvoiceStatus.OPEN.apiId }
            .mapNotNull { it.effectiveOrOriginalDueDate }
            .minOrNull()
    }

    private fun cachedPage(key: PageKey): PagedResult<Invoice>? =
        pageCache[key]
            ?.takeIf { clock.millis() - it.atMillis <= PAGE_CACHE_TTL_MS }
            ?.page

    private fun storePage(key: PageKey, page: PagedResult<Invoice>) {
        val updated = pageCache + (key to PageCacheEntry(page, clock.millis()))
        pageCache = if (updated.size > MAX_CACHED_PAGES) {
            updated - checkNotNull(updated.minByOrNull { it.value.atMillis }).key
        } else {
            updated
        }
    }

    override suspend fun getSecondCopy(contractId: Int): AppResult<SecondCopy> =
        remote.listInvoices(
            contractId = contractId,
            group = InvoiceGroup.OPEN,
            search = "",
            offset = 0,
            limit = 250,
        ).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dtoPage ->
                val copy = dtoPage.items.toSecondCopy()
                if (copy.items.isNotEmpty()) {
                    AppResult.success(copy)
                } else {
                    AppResult.failure(
                        AppError.Api("Não foi possível gerar a segunda via"),
                    )
                }
            },
        )

    override suspend fun sendCopy(
        contractId: Int,
        invoiceId: Int,
        channel: SendChannel,
        destination: String?,
    ): AppResult<String> =
        remote.sendInvoiceCopy(contractId, invoiceId, channel.apiValue).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                if (dto.status == STATUS_OK) {
                    AppResult.success(
                        dto.message ?: "Fatura enviada com sucesso",
                    )
                } else {
                    AppResult.failure(
                        AppError.Api(dto.toMessageOrError()),
                    )
                }
            },
        )

    override suspend fun getInvoiceById(contractId: Int, invoiceId: Int): AppResult<Invoice> {
        lastPage?.takeIf { it.first == contractId }
            ?.second?.firstOrNull { it.id == invoiceId }
            ?.let { return AppResult.success(it) }

        return remote.invoiceDetail(contractId, invoiceId).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                dto.toDomain()
                    ?.withResolvedBoleto()
                    ?.let { AppResult.success(it) }
                    ?: AppResult.failure(AppError.NotFound)
            },
        )
    }

    /** Pre-signed `link` GET — no contract fields, no auth retry. Local
     * boletos come back as PDF; "formato banco" links answer the bank's
     * HTML boleto page, which the UI renders in a web view instead. */
    override suspend fun getBoleto(url: String): AppResult<BoletoContent> =
        remote.downloadBoleto(url).map { bytes ->
            when {
                bytes.isPdf() -> BoletoContent.Pdf(bytes)
                bytes.looksLikeHtml() -> BoletoContent.ExternalPage(url)
                else -> return AppResult.failure(
                    AppError.Api("O boleto ainda não foi gerado para esta fatura. Tente novamente mais tarde."),
                )
            }
        }

    private fun ByteArray.isPdf(): Boolean =
        size >= 4 && this[0] == '%'.code.toByte() && this[1] == 'P'.code.toByte() &&
            this[2] == 'D'.code.toByte() && this[3] == 'F'.code.toByte()

    /** Sniffs an HTML answer (bank boleto page): first non-whitespace
     * byte is `<` (`<!DOCTYPE`, `<html`, …) or a UTF-8 BOM precedes it. */
    private fun ByteArray.looksLikeHtml(): Boolean {
        val head = take(512).toByteArray().toString(Charsets.UTF_8).trimStart('﻿', ' ', '\t', '\n', '\r')
        return head.startsWith("<")
    }

    /** `link`/`chargeUrl` arrive relative to the API host — resolve once so
     * consumers can open the boleto without knowing the base URL. */
    private fun Invoice.withResolvedBoleto(): Invoice =
        copy(
            boletoPath = boletoPath?.let {
                baseUrlProvider.current().resolve(it)?.toString() ?: it
            },
        )

    private companion object {
        const val STATUS_OK = 1
        const val PAGE_CACHE_TTL_MS = 60_000L
        const val MAX_CACHED_PAGES = 16
        const val MAX_KEY_LOCKS = 32
    }
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val remote: CentralAppRemoteDataSource,
) : PaymentRepository {

    override suspend fun generatePix(contractId: Int, invoiceId: Int): AppResult<PixCharge> =
        remote.generatePix(contractId, invoiceId).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                val pix = dto.toDomain()
                if (pix.isSuccess) {
                    AppResult.success(pix)
                } else {
                    AppResult.failure(AppError.Api(pix.message.ifBlank { "PIX indisponível" }))
                }
            },
        )

    override suspend fun requestPaymentPromise(contractId: Int): AppResult<PaymentPromise> =
        remote.paymentPromise(contractId).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                val promise = dto.toPaymentPromise()
                if (promise.status == STATUS_OK || promise.released) {
                    AppResult.success(promise)
                } else {
                    // Business rejection is still a valid answer — surface it as data.
                    if (promise.message.isBlank()) {
                        AppResult.failure(AppError.Api("Promessa não disponível no momento"))
                    } else {
                        AppResult.success(promise)
                    }
                }
            },
        )

    override suspend fun cardCheckoutUrl(contractId: Int, invoiceId: Int): AppResult<String> =
        remote.invoiceDetail(contractId, invoiceId).fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                (dto.chargeUrl ?: dto.boletoUrl)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { AppResult.success(it) }
                    ?: AppResult.failure(AppError.Api("Link de pagamento indisponível"))
            },
        )

    private companion object {
        const val STATUS_OK = 1
    }
}
