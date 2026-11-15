package com.sgp.cliente.data.remote.centralapp

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.network.error.safeApiCall
import com.sgp.cliente.domain.repository.SessionRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Remote data source for the Central App API (`/api/centralapp/`).
 *
 * Auth flow:
 *  - `login()` resolves the ALTCHA challenge when the instance asks for it,
 *    posts `login`+`password`, and returns the issued JWT + profile;
 *  - authenticated calls only need `contrato` — `X-TOKEN`/`Bearer` headers
 *    are merged by [CentralAppAuthInterceptor];
 *  - a 401 triggers ONE silent re-login with the stored credentials, then
 *    the failed call is retried once.
 */
@Singleton
class CentralAppRemoteDataSource @Inject constructor(
    private val api: CentralAppApiService,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val altchaSolver: AltchaSolver,
    private val json: Json,
) {

    // -- bootstrap (pre-login) ------------------------------------------------

    /** Public list of centrals exposed by the configured server. */
    suspend fun listCentrals(): AppResult<List<CentralEntryDto>> =
        safeApiCall { api.listCentrals() }

    /** Config of a central; its token comes from `listCentrals()`. */
    suspend fun centralConfig(central: CentralEntryDto): AppResult<CentralConfigDto> =
        safeApiCall {
            api.centralConfig(central.id, central.token.orEmpty())
        }

    // -- auth -----------------------------------------------------------------

    /**
     * Logs in against `centralapp/login/`. Solves the ALTCHA proof-of-work
     * whenever the challenge endpoint answers — some instances have it off.
     */
    suspend fun login(login: String, password: String): AppResult<LoginResponseDto> {
        val centralToken = settingsRepository.centralToken.first()
            ?: return AppResult.failure(
                AppError.Validation("Servidor do provedor não configurado"),
            )

        val fields = mutableMapOf(
            FIELD_LOGIN to login,
            FIELD_PASSWORD to password,
        )
        solveAltcha(centralToken)?.let { fields[FIELD_ALTCHA] = it }

        return safeApiCall { api.login(fields) }.fold(
            onFailure = { AppResult.failure(it) },
            onSuccess = { dto ->
                if (dto.isSuccess) {
                    AppResult.success(dto)
                } else {
                    AppResult.failure(
                        AppError.Api(dto.message ?: "Credenciais inválidas"),
                    )
                }
            },
        )
    }

    private suspend fun solveAltcha(centralToken: String): String? {
        val challenge = safeApiCall { api.captchaChallenge(centralToken) }
            .fold(
                onSuccess = { it },
                onFailure = { return null },
            )
        return altchaSolver.solve(challenge)
    }

    // -- authenticated calls ----------------------------------------------------

    suspend fun listContracts(): AppResult<List<CentralContractDto>> = callWithAuth { fields ->
        fields[FIELD_PESQUISA] = ""
        api.listContracts(fields).asContractList()
    }

    suspend fun listInvoices(
        contractId: Int,
        group: InvoiceGroup,
        search: String,
        offset: Int,
        limit: Int,
    ): AppResult<CentralInvoicePage> = callWithAuth { fields ->
        fields[FIELD_CONTRATO] = contractId.toString()
        fields[FIELD_GRUPO] = group.apiValue
        fields[FIELD_PESQUISA] = search
        fields[FIELD_LIMIT] = limit.toString()
        fields[FIELD_OFFSET] = offset.toString()
        api.listInvoices(fields).asInvoicePage()
    }

    suspend fun invoiceDetail(
        contractId: Int,
        invoiceId: Int,
    ): AppResult<CentralInvoiceDto> = callWithAuth { fields ->
        fields[FIELD_CONTRATO] = contractId.toString()
        api.invoiceDetail(invoiceId, fields)
    }

    suspend fun generatePix(
        contractId: Int,
        invoiceId: Int,
    ): AppResult<CentralPixResponseDto> = callWithAuth { fields ->
        fields[FIELD_CONTRATO] = contractId.toString()
        api.generatePix(invoiceId, fields)
    }

    suspend fun sendInvoiceCopy(
        contractId: Int,
        invoiceId: Int,
        channel: String,
    ): AppResult<CentralMessageDto> = callWithAuth { fields ->
        fields[FIELD_CONTRATO] = contractId.toString()
        api.sendInvoiceCopy(invoiceId, channel, fields)
    }

    suspend fun paymentPromise(contractId: Int): AppResult<CentralMessageDto> =
        callWithAuth { fields ->
            fields[FIELD_CONTRATO] = contractId.toString()
            fields[FIELD_OBSERVACAO] = ""
            fields[FIELD_TERMO] = "true"
            api.requestPaymentPromise(fields)
        }

    suspend fun listServices(contractId: Int): AppResult<List<CentralServiceDto>> =
        callWithAuth { fields ->
            fields[FIELD_CONTRATO] = contractId.toString()
            api.listServices(fields).asServiceList()
        }

    suspend fun trafficUsage(
        contractId: Int,
        serviceId: Int?,
        year: Int,
        month: Int,
    ): AppResult<CentralStatementDto> = callWithAuth { fields ->
        fields[FIELD_CONTRATO] = contractId.toString()
        fields[FIELD_SERVICO] = serviceId?.toString().orEmpty()
        fields[FIELD_MES] = month.toString()
        fields[FIELD_ANO] = year.toString()
        api.trafficUsage(fields)
    }

    suspend fun serviceStatus(contractId: Int): AppResult<CentralNetworkStatusDto> =
        callWithAuth { fields ->
            fields[FIELD_CONTRATO] = contractId.toString()
            api.networkStatus(fields)
        }

    suspend fun info(contractId: Int?): AppResult<CentralInfoDto> = callWithAuth { fields ->
        contractId?.let { fields[FIELD_CONTRATO] = it.toString() }
        fields["titulo_2via_email"] = "true"
        fields["titulo_2via_sms"] = "true"
        fields["permite_add_pixrec"] = "true"
        api.info(fields)
    }

    /**
     * Downloads a pre-signed boleto PDF (`titulo.link`, resolved to an
     * absolute URL). Plain GET — the link carries its own token, so no
     * `contrato` fields and no `callWithAuth` retry are needed.
     */
    suspend fun downloadBoleto(url: String): AppResult<ByteArray> =
        safeApiCall { api.downloadFile(url).bytes() }

    // -- internals -------------------------------------------------------------

    /**
     * Runs an authenticated call; on 401 re-authenticates once with the
     * stored credentials (JWT re-issue) and retries the call once.
     */
    private suspend fun <T> callWithAuth(
        block: suspend (MutableMap<String, String>) -> T,
    ): AppResult<T> {
        var result = safeApiCall { block(mutableMapOf()) }
        if (result is AppResult.Failure &&
            result.error == AppError.Unauthorized &&
            reLogin()
        ) {
            result = safeApiCall { block(mutableMapOf()) }
        }
        return result
    }

    private val reLoginMutex = Mutex()

    /** Re-issues the JWT with the stored credentials. Serialized — callers queue. */
    private suspend fun reLogin(): Boolean = reLoginMutex.withLock {
        val credentials = sessionRepository.credentials() ?: return@withLock false
        val login = login(credentials.login, credentials.password)
        login.fold(
            onSuccess = { dto ->
                dto.jwtToken?.let { token ->
                    sessionRepository.persistAuthToken(token)
                    true
                } ?: false
            },
            onFailure = { false },
        )
    }

    // JsonElement → typed lists (bare arrays on this server, {result:…} tolerated)
    private fun JsonElement.asContractList(): List<CentralContractDto> =
        json.decodeFromJsonElement(
            ListSerializer(CentralContractDto.serializer()),
            unwrapArray(),
        )

    private fun JsonElement.asInvoicePage(): CentralInvoicePage {
        // Paginated calls answer {"total":N,"result":[...]}; bare calls answer
        // a plain array — keep both shapes working.
        val total = (this as? JsonObject)
            ?.get("total")
            ?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }
        return CentralInvoicePage(
            items = json.decodeFromJsonElement(
                ListSerializer(CentralInvoiceDto.serializer()),
                unwrapArray(),
            ),
            total = total,
        )
    }

    private fun JsonElement.asServiceList(): List<CentralServiceDto> =
        json.decodeFromJsonElement(
            ListSerializer(CentralServiceDto.serializer()),
            unwrapArray(),
        )

    private fun JsonElement.unwrapArray(): JsonElement = when {
        this is JsonArray -> this
        this is JsonObject -> get("result") ?: get("itens") ?: this
        else -> this
    }

    companion object {
        private const val FIELD_LOGIN = "login"
        private const val FIELD_PASSWORD = "password"
        private const val FIELD_ALTCHA = "altcha"
        private const val FIELD_CONTRATO = "contrato"
        private const val FIELD_GRUPO = "grupo"
        private const val FIELD_PESQUISA = "pesquisa"
        private const val FIELD_LIMIT = "limit"
        private const val FIELD_OFFSET = "offset"
        private const val FIELD_OBSERVACAO = "observacao"
        private const val FIELD_TERMO = "termo"
        private const val FIELD_SERVICO = "servico"
        private const val FIELD_MES = "mes"
        private const val FIELD_ANO = "ano"
    }
}

/**
 * `grupo` values we actually send to `titulo/list/`, verified against the
 * live server: `abertos` covers only the payable-now subset (overdue +
 * next-due — the right group for 2ª via), `todas` returns the full
 * stream. Anything else (`canceladas`, `avencer`, …) is silently ignored
 * and falls back to unfiltered, so UI status filters are applied
 * client-side over `todas`.
 */
enum class InvoiceGroup(val apiValue: String) {
    OPEN("abertos"),
    ALL("todas"),
}

/** Parsed `titulo/list/` page — [total] only exists on the `{"total",…,"result":[…]}` envelope. */
data class CentralInvoicePage(
    val items: List<CentralInvoiceDto>,
    val total: Int?,
)
