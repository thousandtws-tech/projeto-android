package com.sgp.cliente.domain.model

import java.time.LocalDate

/** Generic paged collection returned by the SGP API (`paginacao` block). */
data class PagedResult<T>(
    val items: List<T>,
    val offset: Int,
    val limit: Int,
    val total: Int,
) {
    val hasMore: Boolean get() = offset + items.size < total
    val nextOffset: Int get() = offset + items.size
}

/** Result of `POST /api/centralapp/titulo/{id}/pix/`. */
data class PixCharge(
    val status: Int,
    val message: String,
    /** PIX copy-and-paste payload (`pix` field). */
    val code: String?,
) {
    val isSuccess: Boolean get() = status == STATUS_OK && !code.isNullOrBlank()

    private companion object {
        const val STATUS_OK = 1
    }
}

/** Result of `POST /api/centralapp/promessapagamento/create/` ("Liberação por Confiança"). */
data class PaymentPromise(
    val status: Int,
    val protocol: String?,
    /** Whether the service was actually released (`liberado` field). Never assume. */
    val released: Boolean,
    val message: String,
)

/** Second-copy data built from `api/centralapp/titulo/list/` open invoices. */
data class SecondCopy(
    val status: Int,
    val protocol: String?,
    val barcode: String?,
    val link: String?,
    val amount: Double?,
    val dueDate: LocalDate?,
    val originalDueDate: LocalDate?,
    val message: String,
    val items: List<SecondCopyItem>,
)

/** Individual invoice inside a second-copy response (`links` array). */
data class SecondCopyItem(
    val invoiceId: Int,
    val barcode: String?,
    val link: String?,
    val amount: Double?,
    val dueDate: LocalDate?,
    val originalDueDate: LocalDate?,
    val originalAmount: Double?,
)

/** Result of `POST /api/centralapp/rede/status/` — internet service health. */
data class ServiceStatus(
    val statusCode: Int,
    val isOnline: Boolean,
    val message: String,
)

/**
 * Customer credentials for the Central do Assinante.
 * [login] accepts whatever identifier the Central is configured for
 * (`modo_autenticacao`): CPF, CNPJ or the PPPoE/central username.
 * The password is only used at login and silent re-authentication —
 * authenticated calls carry the session JWT instead.
 */
data class Credentials(
    val login: String,
    val password: String,
)

/** Channel used by `POST /api/centralapp/titulo/{id}/{tipo}/send/`. */
enum class SendChannel(val apiValue: String) {
    EMAIL("email"),
    /** SGP also routes "sms" through WhatsApp when configured server-side. */
    SMS("sms"),
}
