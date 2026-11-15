package com.sgp.cliente.data.remote.centralapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// ----- GET /api/centralapp/list/ --------------------------------------------

/** A Central App instance exposed by the SGP server (white-label tenant). */
@Serializable
data class CentralEntryDto(
    @SerialName("id") val id: Int,
    @SerialName("nome") val name: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("logo") val logo: String? = null,
)

// ----- GET /api/centralapp/config/{id}/ -------------------------------------

@Serializable
data class CentralConfigDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("nome") val name: String? = null,
    @SerialName("token") val token: String? = null,
    /** 1 = single combined field accepting CPF, CNPJ or username. */
    @SerialName("modo_autenticacao") val authMode: Int? = null,
    @SerialName("modo_autenticacao_senha") val requiresPassword: Boolean? = null,
    @SerialName("habilitar_captcha_login") val captchaOnLogin: Boolean? = null,
    @SerialName("habilitar_recuperacao_senha") val passwordRecovery: Boolean? = null,
    @SerialName("captcha_challenge_url") val captchaChallengeUrl: String? = null,
    @SerialName("modulos") val modules: List<Int> = emptyList(),
)

// ----- GET /api/centralapp/captcha/challenge?token= -------------------------

/** ALTCHA proof-of-work challenge to solve before logging in. */
@Serializable
data class CaptchaChallengeDto(
    @SerialName("algorithm") val algorithm: String = "SHA-256",
    @SerialName("challenge") val challenge: String,
    @SerialName("salt") val salt: String,
    @SerialName("signature") val signature: String,
    @SerialName("maxnumber") val maxNumber: Int = 1_000_000,
)

// ----- POST /api/centralapp/login/ ------------------------------------------

@Serializable
data class LoginResponseDto(
    @SerialName("status") val status: Int? = null,
    @SerialName("msg") val message: String? = null,
    @SerialName("jwt_token") val jwtToken: String? = null,
    @SerialName("contrato") val contractId: String? = null,
    @SerialName("nome") val customerName: String? = null,
    @SerialName("cpfcnpj") val cpfCnpj: String? = null,
    @SerialName("mudar_senha") val mustChangePassword: Boolean? = null,
    @SerialName("usuario") val user: CentralUserDto? = null,
    @SerialName("modulos") val modules: List<Int>? = null,
) {
    val isSuccess: Boolean get() = status == 1 && !jwtToken.isNullOrBlank()
}

@Serializable
data class CentralUserDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("login") val login: String? = null,
    @SerialName("nome") val name: String? = null,
    @SerialName("administrador") val administrator: Boolean? = null,
)

// ----- POST /api/centralapp/clientecontrato/list/ ---------------------------

@Serializable
data class CentralContractDto(
    @SerialName("id") val id: Int,
    @SerialName("descricao") val description: String? = null,
    @SerialName("cliente_nome") val customerName: String? = null,
    @SerialName("cliente_cpfcnpj") val customerDoc: String? = null,
    @SerialName("situacao") val status: String? = null,
    @SerialName("situacao_data") val statusDate: String? = null,
    @SerialName("vencimento") val dueDay: Int? = null,
    @SerialName("forma_cobranca") val billingMethod: String? = null,
    @SerialName("valor") val value: Double? = null,
    @SerialName("data_contrato") val contractDate: String? = null,
    @SerialName("endereco_linha_0") val address: String? = null,
    @SerialName("servicos") val services: List<CentralContractServiceDto> = emptyList(),
)

@Serializable
data class CentralContractServiceDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("plano") val plan: String? = null,
    @SerialName("grupo") val group: String? = null,
    @SerialName("situacao") val status: String? = null,
    @SerialName("valor") val value: Double? = null,
)

// ----- POST /api/centralapp/titulo/list/ + /titulo/{id}/detail/ -------------

@Serializable
data class CentralInvoiceDto(
    @SerialName("id") val id: Int,
    @SerialName("clientecontrato") val contractId: Int? = null,
    @SerialName("numero_documento") val documentNumber: Long? = null,
    @SerialName("descricao") val description: String? = null,
    @SerialName("situacao") val status: String? = null,
    @SerialName("situacao_atrasada_dias") val overdueDays: Int? = null,
    @SerialName("data_vencimento") val dueDate: String? = null,
    @SerialName("data_pagamento") val paidAt: String? = null,
    @SerialName("data_documento") val issuedAt: String? = null,
    @SerialName("valor") val amount: String? = null,
    @SerialName("valortotal") val totalAmount: String? = null,
    @SerialName("valorpago") val paidAmount: String? = null,
    @SerialName("acrescimo") val surcharge: String? = null,
    @SerialName("juros") val interest: String? = null,
    @SerialName("multa") val fine: String? = null,
    @SerialName("desconto") val discount: String? = null,
    @SerialName("linha_digitavel") val barcode: String? = null,
    @SerialName("codigo_barras") val barcodeAlt: String? = null,
    @SerialName("codigo_pix") val pixCode: String? = null,
    @SerialName("link") val boletoUrl: String? = null,
    @SerialName("url_cobranca") val chargeUrl: String? = null,
    @SerialName("comprovante_link") val receiptUrl: String? = null,
    @SerialName("idpublico") val publicId: String? = null,
    @SerialName("pagamento_pix") val allowsPix: Boolean? = null,
    @SerialName("pix_sob_demanda") val pixOnDemand: Boolean? = null,
    @SerialName("pagamento_boleto") val allowsBoleto: Boolean? = null,
    @SerialName("pagamento_cartao") val allowsCard: Boolean? = null,
    @SerialName("pagamento_credito") val allowsCredit: Boolean? = null,
    @SerialName("pagamento_debito") val allowsDebit: Boolean? = null,
    @SerialName("emails") val emails: List<String> = emptyList(),
    @SerialName("celulares") val phones: List<String> = emptyList(),
)

// ----- POST /api/centralapp/titulo/{id}/pix/ --------------------------------

@Serializable
data class CentralPixResponseDto(
    @SerialName("status") val status: Int? = null,
    @SerialName("msg") val message: String? = null,
    /** Shape varies by gateway — kept as raw JSON and picked apart downstream. */
    @SerialName("pix") val pix: JsonObject? = null,
) {
    /** First non-blank copy-paste code found under the common key names. */
    fun copyPasteCode(): String? =
        pix?.entries?.asSequence()
            ?.mapNotNull { (key, value) ->
                value.jsonPrimitive.content
                    .takeIf { it.isNotBlank() }
                    ?.takeIf {
                        key.equals("codigo", true) ||
                            key.equals("copiaecola", true) ||
                            key.equals("copia_e_cola", true) ||
                            key.equals("emv", true) ||
                            key.equals("payload", true) ||
                            key.equals("pix", true) ||
                            key.equals("codigo_pix", true) ||
                            key.equals("qrcode", true)
                    }
            }
            ?.firstOrNull()
}

// ----- Shared {status, msg} response ----------------------------------------

@Serializable
data class CentralMessageDto(
    @SerialName("status") val status: Int? = null,
    @SerialName("msg") val message: String? = null,
    @SerialName("protocolo") val protocol: String? = null,
)

// ----- POST /api/centralapp/servico/list -------------------------------------

@Serializable
data class CentralServiceDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("plano") val plan: String? = null,
    @SerialName("login") val login: String? = null,
    @SerialName("situacao") val status: String? = null,
    @SerialName("tipo") val type: String? = null,
    @SerialName("grupo") val group: String? = null,
    @SerialName("up") val uploadLabel: String? = null,
    @SerialName("down") val downloadLabel: String? = null,
    @SerialName("clientecontrato_id") val contractId: Int? = null,
)

// ----- POST /api/centralapp/extrato/list/ ------------------------------------

@Serializable
data class CentralStatementDto(
    @SerialName("status") val status: Int? = null,
    @SerialName("msg") val message: String? = null,
    @SerialName("extrato") val statement: CentralStatementDataDto? = null,
)

@Serializable
data class CentralStatementDataDto(
    @SerialName("itens") val items: List<CentralStatementItemDto> = emptyList(),
    @SerialName("total") val totalLabel: String? = null,
    @SerialName("mes") val monthLabel: String? = null,
    @SerialName("ano") val year: Int? = null,
)

@Serializable
data class CentralStatementItemDto(
    @SerialName("data") val date: String? = null,
    @SerialName("dia") val day: String? = null,
    @SerialName("download") val download: String? = null,
    @SerialName("upload") val upload: String? = null,
    @SerialName("total") val total: String? = null,
)

// ----- POST /api/centralapp/rede/status/ -------------------------------------

@Serializable
data class CentralNetworkStatusDto(
    @SerialName("status") val status: Int? = null,
    @SerialName("disponivel") val available: Boolean? = null,
    @SerialName("msg") val message: String? = null,
)

// ----- POST /api/centralapp/info/ --------------------------------------------

/** Feature flags of the contract (2nd-copy channels, PIX recurrence, status). */
@Serializable
data class CentralInfoDto(
    @SerialName("titulo_2via_email") val secondCopyEmail: Boolean? = null,
    @SerialName("titulo_2via_sms") val secondCopySms: Boolean? = null,
    @SerialName("permite_add_pixrec") val allowsPixRecurrence: Boolean? = null,
    @SerialName("ocorrencia_cadastrar") val allowsTickets: Boolean? = null,
    @SerialName("chat_habilitado") val chatEnabled: Boolean? = null,
    @SerialName("contrato_status") val contractStatus: String? = null,
)
