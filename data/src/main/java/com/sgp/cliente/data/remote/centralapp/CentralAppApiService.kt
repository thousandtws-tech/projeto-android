package com.sgp.cliente.data.remote.centralapp

import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Central App API (`/api/centralapp/`) — the endpoints used by the current
 * Central do Assinante. Auth model:
 *  - `X-TOKEN` header: the central's token (from `list/` / `config/{id}/`),
 *    added by the auth interceptor once selected;
 *  - `Authorization: Bearer <jwt>`: issued by `login/`, added by the same
 *    interceptor (skipped on bootstrap endpoints);
 *  - `contrato` form field: selected contract id, merged per call.
 */
interface CentralAppApiService {

    /** Public: centrals (branded tenants) exposed by this server. */
    @GET("api/centralapp/list/")
    suspend fun listCentrals(): List<CentralEntryDto>

    /** Central config; requires its own token — passed explicitly here. */
    @GET("api/centralapp/config/{id}/")
    suspend fun centralConfig(
        @Path("id") centralId: Int,
        @Header("X-TOKEN") centralToken: String,
    ): CentralConfigDto

    /** ALTCHA challenge for the login proof-of-work. */
    @GET("api/centralapp/captcha/challenge")
    suspend fun captchaChallenge(
        @Query("token") centralToken: String,
    ): CaptchaChallengeDto

    /** Login. Fields: login, password[, altcha]. */
    @FormUrlEncoded
    @POST("api/centralapp/login/")
    suspend fun login(@FieldMap fields: Map<String, String>): LoginResponseDto

    /**
     * Contract list of the customer. The endpoint answers a bare array on
     * this server; [JsonElement] keeps us resilient if a `{"result":…}`
     * envelope shows up on other versions.
     */
    @FormUrlEncoded
    @POST("api/centralapp/clientecontrato/list/")
    suspend fun listContracts(@FieldMap fields: Map<String, String>): JsonElement

    /** Invoice list. Fields: contrato, grupo (abertos|pagos|todos), pesquisa, limit, offset. */
    @FormUrlEncoded
    @POST("api/centralapp/titulo/list/")
    suspend fun listInvoices(@FieldMap fields: Map<String, String>): JsonElement

    /** Full detail of one invoice. Fields: contrato. */
    @FormUrlEncoded
    @POST("api/centralapp/titulo/{id}/detail/")
    suspend fun invoiceDetail(
        @Path("id") invoiceId: Int,
        @FieldMap fields: Map<String, String>,
    ): CentralInvoiceDto

    /** Generates/loads the on-demand PIX for one invoice. Fields: contrato. */
    @FormUrlEncoded
    @POST("api/centralapp/titulo/{id}/pix/")
    suspend fun generatePix(
        @Path("id") invoiceId: Int,
        @FieldMap fields: Map<String, String>,
    ): CentralPixResponseDto

    /** Sends the invoice copy. tipo = email|sms. Fields: contrato. */
    @FormUrlEncoded
    @POST("api/centralapp/titulo/{id}/{tipo}/send/")
    suspend fun sendInvoiceCopy(
        @Path("id") invoiceId: Int,
        @Path("tipo") channel: String,
        @FieldMap fields: Map<String, String>,
    ): CentralMessageDto

    /** "Liberação por confiança". Fields: contrato, observacao, termo. */
    @FormUrlEncoded
    @POST("api/centralapp/promessapagamento/create/")
    suspend fun requestPaymentPromise(
        @FieldMap fields: Map<String, String>,
    ): CentralMessageDto

    /** Services of the contract (internet/telephony plans + service ids). */
    @FormUrlEncoded
    @POST("api/centralapp/servico/list")
    suspend fun listServices(@FieldMap fields: Map<String, String>): JsonElement

    /** Monthly traffic report. Fields: contrato, servico, mes, ano. */
    @FormUrlEncoded
    @POST("api/centralapp/extrato/list/")
    suspend fun trafficUsage(@FieldMap fields: Map<String, String>): CentralStatementDto

    /** Network availability for the contract. Fields: contrato. */
    @FormUrlEncoded
    @POST("api/centralapp/rede/status/")
    suspend fun networkStatus(@FieldMap fields: Map<String, String>): CentralNetworkStatusDto

    /** Capability flags (2nd-copy channels, PIX recurrence, contract status). */
    @FormUrlEncoded
    @POST("api/centralapp/info/")
    suspend fun info(@FieldMap fields: Map<String, String>): CentralInfoDto

    /**
     * Raw download of a pre-signed resource on this host — invoice boleto
     * PDFs (`link`/`url_cobranca` from `titulo`). Absolute `@Url` bypasses
     * `baseUrl`; the auth interceptor only signs `/api/centralapp/` paths.
     */
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}
