package com.sgp.cliente.data.remote.centralapp

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Wire-level contract tests for the Central App API (`/api/centralapp/`):
 * verifies the exact field names/paths the real server expects and that
 * live response shapes deserialize into our DTOs.
 */
class CentralAppApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: CentralAppApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CentralAppApiService::class.java)
    }

    @After
    fun tearDown() = server.close()

    private fun enqueueJson(body: String) {
        server.enqueue(
            MockResponse.Builder()
                .body(body)
                .addHeader("Content-Type", "application/json")
                .build(),
        )
    }

    @Test
    fun `list centrals parses the tenant array`() = runTest {
        enqueueJson(
            """[{"id":3,"nome":"Central do Assinante","token":"t-3"},
               {"id":51,"nome":"hotspot","token":"t-51"}]""",
        )

        val centrals = api.listCentrals()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.url.encodedPath.endsWith("api/centralapp/list/"))
        assertEquals(2, centrals.size)
        assertEquals(3, centrals[0].id)
        assertEquals("Central do Assinante", centrals[0].name)
        assertEquals("t-3", centrals[0].token)
    }

    @Test
    fun `central config sends X-TOKEN header`() = runTest {
        enqueueJson(
            """{"id":3,"nome":"Central do Assinante","modo_autenticacao":1,
               "modo_autenticacao_senha":true,"habilitar_captcha_login":true,
               "modulos":[1,2,3,4,5,6,11,12]}""",
        )

        val config = api.centralConfig(3, "t-3")

        val request = server.takeRequest()
        assertTrue(request.url.encodedPath.endsWith("api/centralapp/config/3/"))
        assertEquals("t-3", request.headers["X-TOKEN"])
        assertEquals(1, config.authMode)
        assertEquals(true, config.captchaOnLogin)
    }

    @Test
    fun `captcha challenge parses PoW fields`() = runTest {
        enqueueJson(
            """{"algorithm":"SHA-256","challenge":"abc","salt":"s1",
               "signature":"sig","maxnumber":1000000}""",
        )

        val challenge = api.captchaChallenge("t-3")

        val url = server.takeRequest().url
        assertTrue(url.encodedPath.endsWith("api/centralapp/captcha/challenge"))
        assertEquals("t-3", url.queryParameter("token"))
        assertEquals("SHA-256", challenge.algorithm)
        assertEquals(1_000_000, challenge.maxNumber)
    }

    @Test
    fun `login sends login password altcha as form fields`() = runTest {
        enqueueJson(
            """{"status":1,"jwt_token":"jwt.here","contrato":"566",
               "nome":"PEDRO","cpfcnpj":"00000000000","mudar_senha":true,
               "usuario":{"login":"6zta35f0"},"modulos":[1,2]}""",
        )

        val response = api.login(
            mapOf("login" to "6zta35f0", "password" to "secret", "altcha" to "enc"),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.url.encodedPath.endsWith("api/centralapp/login/"))
        val body = request.body?.utf8().orEmpty()
        assertTrue(body.contains("login=6zta35f0"))
        assertTrue(body.contains("password=secret"))
        assertTrue(body.contains("altcha=enc"))

        assertTrue(response.isSuccess)
        assertEquals("jwt.here", response.jwtToken)
        assertEquals("566", response.contractId)
        assertEquals("PEDRO", response.customerName)
    }

    @Test
    fun `invoice list sends grupo pesquisa limit offset`() = runTest {
        enqueueJson(
            """[{"id":13658,"clientecontrato":566,"situacao":"Em aberto",
               "data_vencimento":"10/11/2025","valor":"99.90",
               "valortotal":"109.90","linha_digitavel":"12345",
               "pagamento_pix":true}]""",
        )

        val response = api.listInvoices(
            mapOf(
                "contrato" to "566", "grupo" to "abertos",
                "pesquisa" to "", "limit" to "250", "offset" to "0",
            ),
        )

        val body = server.takeRequest().body?.utf8().orEmpty()
        assertTrue(body.contains("contrato=566"))
        assertTrue(body.contains("grupo=abertos"))
        assertTrue(body.contains("limit=250"))

        val invoices = response.jsonArray
        assertEquals(1, invoices.size)
    }

    @Test
    fun `pix endpoint interpolates invoice id and parses object payload`() = runTest {
        enqueueJson(
            """{"status":1,"msg":"PIX gerado","pix":{"codigo":"000201pix"}}""",
        )

        val response = api.generatePix(77, mapOf("contrato" to "566"))

        val request = server.takeRequest()
        assertTrue(request.url.encodedPath.endsWith("api/centralapp/titulo/77/pix/"))
        assertEquals(1, response.status)
        assertEquals("000201pix", response.copyPasteCode())
    }

    @Test
    fun `send copy interpolates invoice id and channel`() = runTest {
        enqueueJson("""{"status":1,"msg":"Enviado","protocolo":"P-9"}""")

        val response = api.sendInvoiceCopy(9001, "email", mapOf("contrato" to "566"))

        val request = server.takeRequest()
        assertTrue(request.url.encodedPath.endsWith("api/centralapp/titulo/9001/email/send/"))
        assertEquals("P-9", response.protocol)
    }

    @Test
    fun `login failure payload deserializes with status zero`() = runTest {
        enqueueJson("""{"status":0,"msg":"Login ou senha inválidos."}""")

        val response = api.login(mapOf("login" to "x", "password" to "y"))
        assertEquals(false, response.isSuccess)
        assertEquals("Login ou senha inválidos.", response.message)
    }
}
