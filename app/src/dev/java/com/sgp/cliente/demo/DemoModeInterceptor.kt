package com.sgp.cliente.demo

import com.sgp.cliente.core.network.api.BaseUrlProvider
import java.net.URLDecoder
import javax.inject.Inject
import okio.Buffer
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Dev-flavor-only offline demo mode. When the configured base URL host is
 * `demo` (type `demo` in the server screen), every `/api/centralapp/`
 * endpoint answers with canned payloads matching the real contract —
 * no network is touched. Never compiled into staging/prod builds.
 *
 * Demo login accepts any non-blank credentials and issues a fake JWT.
 */
class DemoModeInterceptor @Inject constructor(
    private val baseUrlProvider: BaseUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (baseUrlProvider.current().host != DEMO_HOST) {
            return chain.proceed(chain.request())
        }
        // Simulated latency keeps loading states (skeletons) observable.
        Thread.sleep(SIMULATED_LATENCY_MS)
        val path = chain.request().url.encodedPath

        // The demo central has no captcha — a 404 makes login skip ALTCHA,
        // exactly like instances with `habilitar_captcha_login` off.
        if (path.contains("api/centralapp/captcha/challenge")) {
            return response(chain.request(), 404, """{"detail":"no captcha"}""")
        }

        // Boleto downloads (`link` in the canned invoices) answer with a
        // minimal real PDF so the in-app viewer can render it offline.
        if (path.endsWith(".pdf")) {
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(DEMO_PDF.toResponseBody(PDF))
                .build()
        }

        val body = demoPayload(path, chain.request())
        return response(chain.request(), 200, body)
    }

    private fun response(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(body.toResponseBody(JSON))
            .build()

    private fun demoPayload(path: String, request: Request): String = when {
        path.endsWith("api/centralapp/list/") -> CENTRALS
        path.contains("api/centralapp/config/") -> CONFIG
        path.endsWith("api/centralapp/login/") -> loginPayload(request)
        path.endsWith("api/centralapp/clientecontrato/list/") -> CONTRACTS
        path.endsWith("api/centralapp/titulo/list/") ->
            invoices(request.formField("grupo"), request.formField("contrato"))
        path.contains(Regex("api/centralapp/titulo/\\d+/detail/")) -> INVOICE_DETAIL
        path.contains(Regex("api/centralapp/titulo/\\d+/pix/")) -> PIX
        path.contains(Regex("api/centralapp/titulo/\\d+/(email|sms)/send/")) -> SEND_COPY
        path.endsWith("api/centralapp/promessapagamento/create/") -> PROMISE
        path.endsWith("api/centralapp/servico/list") -> SERVICES
        path.endsWith("api/centralapp/extrato/list/") -> STATEMENT
        path.endsWith("api/centralapp/rede/status/") -> NETWORK_STATUS
        path.endsWith("api/centralapp/info/") -> INFO
        else -> """{"status":1,"msg":"ok"}"""
    }

    /** Demo accepts any identifier; a blank one answers like a bad login. */
    private fun loginPayload(request: Request): String =
        if (request.formField("login").isNullOrBlank() ||
            request.formField("password").isNullOrBlank()
        ) {
            """{"status":0,"msg":"Login ou senha inválidos."}"""
        } else {
            LOGIN
        }

    /** Reads a field out of a `FormUrlEncoded` request body. */
    private fun Request.formField(name: String): String? {
        val body = body ?: return null
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
                .split('&')
                .firstNotNullOfOrNull { part ->
                    val key = part.substringBefore('=')
                    val value = part.substringAfter('=', "")
                    @Suppress("DEPRECATION")
                    if (key == name) URLDecoder.decode(value, "UTF-8") else null
                }
        }.getOrNull()
    }

    // Contract 102 is suspended (hidden by the active-only picker) and
    // has no boletos; 103 is a second active one at another address.
    private fun invoices(group: String?, contractId: String?): String = when {
        contractId == "102" -> "[]"
        group == "pagos" -> """[$INVOICE_PAID_1,$INVOICE_PAID_2]"""
        group == "abertos" -> """[$INVOICE_OPEN_1,$INVOICE_OPEN_2,$INVOICE_OPEN_PIX]"""
        group == "canceladas" -> """[$INVOICE_CANCELLED]"""
        else -> INVOICES_ALL
    }

    companion object {
        const val DEMO_HOST = "demo"
        private const val SIMULATED_LATENCY_MS = 400L
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val PDF = "application/pdf".toMediaType()

        /** Smallest one-page PDF PDFium will render (correct xref offsets). */
        private val DEMO_PDF: ByteArray = run {
            val text = "BT /F1 20 Tf 60 720 Td (BOLETO DEMO) Tj ET"
            val objects = listOf(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                    "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length ${text.length} >>\nstream\n$text\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            )
            val sb = StringBuilder("%PDF-1.4\n")
            val offsets = IntArray(objects.size + 1)
            objects.forEachIndexed { i, obj ->
                offsets[i + 1] = sb.length
                sb.append("${i + 1} 0 obj\n$obj\nendobj\n")
            }
            val xref = sb.length
            sb.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
            (1..objects.size).forEach { sb.append("%010d 00000 n \n".format(offsets[it])) }
            sb.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
            sb.append("startxref\n$xref\n%%EOF\n")
            sb.toString().toByteArray(Charsets.US_ASCII)
        }

        private const val CENTRALS = """[
            {"id": 3, "nome": "Central do Assinante", "token": "demo-central-token"}
        ]"""

        private const val CONFIG = """{
            "id": 3, "nome": "Central do Assinante", "token": "demo-central-token",
            "modo_autenticacao": 1, "modo_autenticacao_senha": true,
            "habilitar_captcha_login": false, "habilitar_recuperacao_senha": true,
            "modulos": [1,2,3,4,5,6,11,12]
        }"""

        private const val LOGIN = """{
            "status": 1, "msg": "ok",
            "jwt_token": "demo.jwt.token",
            "contrato": "101", "nome": "Cliente Demo",
            "cpfcnpj": "52998224725", "mudar_senha": false,
            "usuario": {"id": 7, "login": "cliente.demo", "nome": "Cliente Demo"},
            "modulos": [1,2,3,4,5,6,11,12]
        }"""

        private const val CONTRACTS = """[
            {"id": 101, "descricao": "Contrato residencial",
             "cliente_nome": "Cliente Demo", "cliente_cpfcnpj": "52998224725",
             "situacao": "Ativo", "vencimento": 10, "valor": 119.9,
             "endereco_linha_0": "Rua das Flores, 123 - Centro",
             "servicos": [
                {"id": 501, "plano": "Fibra 300 Mega", "grupo": "internet",
                 "situacao": "Ativo", "valor": 119.9}
             ]},
            {"id": 102, "descricao": "Contrato antigo",
             "cliente_nome": "Cliente Demo", "cliente_cpfcnpj": "52998224725",
             "situacao": "Suspenso", "vencimento": 20,
             "endereco_linha_0": "Av. Brasil, 500 - Bairro Alto",
             "servicos": [
                {"id": 502, "plano": "Fibra 100 Mega", "grupo": "internet",
                 "situacao": "Suspenso", "valor": 89.9}
             ]},
            {"id": 103, "descricao": "Contrato loja",
             "cliente_nome": "Cliente Demo", "cliente_cpfcnpj": "52998224725",
             "situacao": "Ativo", "vencimento": 15, "valor": 149.9,
             "endereco_linha_0": "Rua do Comércio, 77 - Vila Nova",
             "servicos": [
                {"id": 503, "plano": "Fibra 500 Mega", "grupo": "internet",
                 "situacao": "Ativo", "valor": 149.9}
             ]}
        ]"""

        private const val INVOICE_OPEN_1 = """{
            "id": 9001, "clientecontrato": 101, "numero_documento": 55401,
            "situacao": "Em aberto", "data_vencimento": "10/10/2025",
            "valor": "119.90", "valortotal": "131.90",
            "linha_digitavel": "23793.38128 60007.827136 95000.063305 9 89000000013190",
            "codigo_barras": "23799890000000131903381600078271369500006330",
            "link": "https://demo.local/fatura/9001.pdf",
            "url_cobranca": "https://demo.local/cobranca/9001",
            "pagamento_pix": true, "pix_sob_demanda": true,
            "pagamento_boleto": true, "pagamento_cartao": true,
            "emails": ["cliente.demo@exemplo.com"], "celulares": ["5511999990000"]
        }"""

        private const val INVOICE_OPEN_2 = """{
            "id": 9005, "clientecontrato": 101, "numero_documento": 55405,
            "situacao": "Em aberto", "data_vencimento": "01/11/2025",
            "valor": "119.90", "valortotal": "125.90",
            "linha_digitavel": "23793.38128 60007.827136 95000.063305 9 89000000012590",
            "pagamento_pix": true, "pix_sob_demanda": true,
            "pagamento_boleto": true,
            "emails": ["cliente.demo@exemplo.com"], "celulares": []
        }"""

        private const val INVOICE_OPEN_PIX = """{
            "id": 9002, "clientecontrato": 101, "numero_documento": 55402,
            "situacao": "Em aberto", "data_vencimento": "10/12/2025",
            "valor": "119.90", "valortotal": "119.90",
            "codigo_pix": "00020126580014br.gov.bcb.pix0136demo-9002",
            "pagamento_pix": true, "pagamento_boleto": false,
            "emails": [], "celulares": []
        }"""

        private const val INVOICE_PAID_1 = """{
            "id": 9003, "clientecontrato": 101, "numero_documento": 55403,
            "situacao": "Pago", "data_vencimento": "10/09/2025",
            "data_pagamento": "08/09/2025",
            "valor": "119.90", "valortotal": "119.90", "valorpago": "119.90",
            "comprovante_link": "https://demo.local/comprovante/9003.pdf",
            "emails": [], "celulares": []
        }"""

        private const val INVOICE_PAID_2 = """{
            "id": 9004, "clientecontrato": 101, "numero_documento": 55404,
            "situacao": "Pago", "data_vencimento": "10/10/2024",
            "data_pagamento": "09/10/2024",
            "valor": "119.90", "valortotal": "119.90", "valorpago": "119.90",
            "emails": [], "celulares": []
        }"""

        private const val INVOICE_CANCELLED = """{
            "id": 9006, "clientecontrato": 101, "numero_documento": 55406,
            "situacao": "Cancelado", "data_vencimento": "10/08/2025",
            "valor": "119.90", "emails": [], "celulares": []
        }"""

        private const val INVOICES_ALL =
            """[$INVOICE_OPEN_1,$INVOICE_OPEN_2,$INVOICE_OPEN_PIX,$INVOICE_PAID_1,$INVOICE_PAID_2,$INVOICE_CANCELLED]"""

        private const val INVOICE_DETAIL = """{
            "id": 9001, "clientecontrato": 101, "numero_documento": 55401,
            "descricao": "Mensalidade Fibra 300 Mega",
            "situacao": "Em aberto", "data_vencimento": "10/10/2025",
            "valor": "119.90", "valortotal": "131.90",
            "acrescimo": "12.00", "juros": "0.00", "multa": "0.00",
            "linha_digitavel": "23793.38128 60007.827136 95000.063305 9 89000000013190",
            "codigo_barras": "23799890000000131903381600078271369500006330",
            "link": "https://demo.local/fatura/9001.pdf",
            "url_cobranca": "https://demo.local/cobranca/9001",
            "pagamento_pix": true, "pix_sob_demanda": true,
            "pagamento_boleto": true, "pagamento_cartao": true,
            "emails": ["cliente.demo@exemplo.com"], "celulares": ["5511999990000"]
        }"""

        private const val PIX = """{
            "status": 1, "msg": "PIX gerado com sucesso",
            "pix": {"codigo": "00020126580014br.gov.bcb.pix0136demo-pix-gerado-5204000053039865405125.905802BR5925PROVEDOR DEMO6009SAO PAULO62070503***6304DEMO"}
        }"""

        private const val SEND_COPY = """{
            "status": 1, "protocolo": "DEMO-ENV-001",
            "msg": "Segunda via enviada para o e-mail cadastrado."
        }"""

        private const val PROMISE = """{
            "status": 1, "protocolo": "DEMO-PROM-777",
            "msg": "Promessa registrada. Acesso liberado por 5 dias."
        }"""

        private const val SERVICES = """[
            {"id": 501, "plano": "Fibra 300 Mega", "login": "cliente.demo",
             "situacao": "Ativo", "tipo": "Internet", "grupo": "internet",
             "up": "150 Mb", "down": "300 Mb", "clientecontrato_id": 101}
        ]"""

        private const val STATEMENT = """{
            "status": 1, "msg": "ok",
            "extrato": {
                "total": "152.4 GB",
                "mes": "Novembro", "ano": 2025,
                "itens": [
                    {"data": "12/11/2025", "download": "12.4 GB", "upload": "1.1 GB", "total": "13.5 GB"},
                    {"data": "11/11/2025", "download": "8.7 GB", "upload": "0.9 GB", "total": "9.6 GB"}
                ]
            }
        }"""

        private const val NETWORK_STATUS = """{
            "status": 1, "disponivel": true, "msg": "Serviço online"
        }"""

        private const val INFO = """{
            "titulo_2via_email": true, "titulo_2via_sms": true,
            "permite_add_pixrec": true, "ocorrencia_cadastrar": true,
            "chat_habilitado": false, "contrato_status": "Ativo"
        }"""
    }
}
