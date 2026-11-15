package com.sgp.cliente.domain.repository

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.CentralOption
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PagedResult
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.model.SecondCopy
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

/** Owns the persisted session. Implemented in :core:datastore. */
interface SessionRepository {
    val state: Flow<SessionState>

    suspend fun persist(session: CustomerSession)
    suspend fun updateContracts(contracts: List<Contract>)
    suspend fun selectContract(contractId: Int)
    suspend fun credentials(): Credentials?
    suspend fun selectedContractId(): Int?
    suspend fun selectedContract(): Contract?

    /** Central App JWT used as `Authorization: Bearer` on authenticated calls. */
    suspend fun authToken(): String?

    suspend fun persistAuthToken(token: String)
    suspend fun clear()
}

interface AuthRepository {
    /**
     * Authenticates against `POST /api/centralapp/login/`
     * (identifier = CPF, CNPJ or central username, plus password and the
     * ALTCHA proof-of-work when the instance requires it).
     * Success also delivers the customer's contract list.
     */
    suspend fun login(login: String, password: String): AppResult<CustomerSession>

    /**
     * Lists the centrals exposed by [baseUrl]
     * (`GET /api/centralapp/list/`), selects the best match and stores its
     * id+token+name — required before any authenticated call can run.
     * Passing the URL explicitly keeps this independent of the reactive
     * base-URL propagation.
     */
    suspend fun discoverAndSelectCentral(baseUrl: String): AppResult<CentralOption>
}

interface ContractRepository {
    /** Re-fetches `POST /api/centralapp/clientecontrato/list/`. */
    suspend fun refreshContracts(): AppResult<List<Contract>>

    /** `POST /api/centralapp/rede/status/` — whether the internet service is online. */
    suspend fun checkServiceStatus(contractId: Int): AppResult<ServiceStatus>
}

interface InvoiceRepository {
    /**
     * `POST /api/centralapp/titulo/list/`.
     * [statusFilter] uses the SGP codes (1=aberto, 2=pago, 3=cancelado) and
     * is applied client-side — the server's `grupo` values cannot express
     * them (`abertos` is only the payable-now subset; `canceladas` is
     * ignored), so the raw `todas` stream is paged and filtered locally.
     */
    suspend fun getInvoices(
        contractId: Int,
        statusFilter: Int? = null,
        offset: Int = 0,
        limit: Int = 250,
        forceRefresh: Boolean = false,
    ): AppResult<PagedResult<Invoice>>

    /** Open/overdue invoices with fresh billet links (`titulo/list` grupo=abertos). */
    suspend fun getSecondCopy(contractId: Int): AppResult<SecondCopy>

    /** `POST /api/centralapp/titulo/{id}/{tipo}/send/` — e-mail or SMS/WhatsApp. */
    suspend fun sendCopy(
        contractId: Int,
        invoiceId: Int,
        channel: SendChannel,
        destination: String? = null,
    ): AppResult<String>

    /** `POST /api/centralapp/titulo/{id}/detail/` (with page-cache fallback). */
    suspend fun getInvoiceById(contractId: Int, invoiceId: Int): AppResult<Invoice>

    /**
     * Raw PDF bytes of the invoice boleto. [url] is the invoice's resolved
     * `boletoPath` (`link`/`url_cobranca`) — a pre-signed GET on the same
     * host, so no contract fields or auth retry are needed.
     */
    suspend fun getBoletoPdf(url: String): AppResult<ByteArray>
}

interface PaymentRepository {
    /** `POST /api/centralapp/titulo/{id}/pix/` — generates a PIX copy-and-paste code. */
    suspend fun generatePix(contractId: Int, invoiceId: Int): AppResult<PixCharge>

    /** `POST /api/centralapp/promessapagamento/create/` — "liberação por confiança". */
    suspend fun requestPaymentPromise(contractId: Int): AppResult<PaymentPromise>

    /** Public payment page of the invoice (`url_cobranca` from the detail). */
    suspend fun cardCheckoutUrl(contractId: Int, invoiceId: Int): AppResult<String>
}

/** App-level settings. Implemented in :core:datastore. */
interface SettingsRepository {
    /** Overrides the flavor-provided default base URL. `null` = use default. */
    val baseUrlOverride: Flow<String?>

    /**
     * Selected Central App instance (`GET /api/centralapp/list/`).
     * Its token is sent as the `X-TOKEN` header on every centralapp call.
     */
    val centralId: Flow<Int?>

    /** The selected central's app token (`X-TOKEN`). */
    val centralToken: Flow<String?>

    /** Whether the first-run onboarding has been shown. */
    val onboardingSeen: Flow<Boolean>

    /** Local notification preference: invoice due-date alerts. */
    val invoiceAlertsEnabled: Flow<Boolean>

    /** Local notification preference: support-ticket updates. */
    val ticketUpdatesEnabled: Flow<Boolean>

    suspend fun setBaseUrlOverride(url: String?)
    suspend fun setCentral(id: Int?, token: String?)
    suspend fun setOnboardingSeen()
    suspend fun setNotificationPrefs(invoiceAlerts: Boolean, ticketUpdates: Boolean)
}
