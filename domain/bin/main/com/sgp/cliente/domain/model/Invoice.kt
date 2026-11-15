package com.sgp.cliente.domain.model

import java.time.LocalDate

/**
 * Invoice/bill (`titulo`) returned by `POST /api/centralapp/titulo/list/`.
 *
 * [statusId] follows the SGP contract: 1 = em aberto, 2 = pago, 3 = cancelado.
 */
data class Invoice(
    val id: Int,
    val documentNumber: Long?,
    val statusId: Int,
    val statusLabel: String,
    /** Original due date (`vencimento`). */
    val dueDate: LocalDate?,
    /** Recalculated due date (`vencimento_atualizado`), already with agreements/extensions applied. */
    val effectiveDueDate: LocalDate?,
    /** Original amount (`valor`). */
    val amount: Double?,
    /** Amount corrected with interest/fees (`valorcorrigido`). This is what the customer pays. */
    val correctedAmount: Double?,
    val paidAt: LocalDate?,
    /** Digitable line of the boleto (`linhadigitavel`). */
    val barcode: String?,
    /** PIX copy-and-paste code returned inline by the list (`codigopix`). */
    val pixCode: String?,
    /** Whether the contract allows on-demand PIX generation (`gerarpix`). */
    val canGeneratePix: Boolean,
    /** Absolute boleto URL (`link`), resolved against the API base URL at ingest. */
    val boletoPath: String?,
) {
    val status: InvoiceStatus
        get() = InvoiceStatus.fromId(statusId)

    /** Amount shown to the customer: corrected when available. */
    val displayAmount: Double?
        get() = correctedAmount ?: amount

    val effectiveOrOriginalDueDate: LocalDate?
        get() = effectiveDueDate ?: dueDate

    /** Open invoice whose (effective) due date is already in the past. */
    fun isOverdue(today: LocalDate): Boolean =
        status == InvoiceStatus.OPEN &&
            effectiveOrOriginalDueDate?.isBefore(today) == true

    /** Competence key derived from the original due date (year-month). */
    val competenceYearMonth: String?
        get() = dueDate?.let { "%04d-%02d".format(it.year, it.monthValue) }
}

enum class InvoiceStatus(val apiId: Int) {
    OPEN(1),
    PAID(2),
    CANCELLED(3),
    UNKNOWN(-1);

    companion object {
        fun fromId(id: Int): InvoiceStatus = entries.firstOrNull { it.apiId == id } ?: UNKNOWN
    }
}
