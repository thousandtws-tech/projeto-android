package com.sgp.cliente.data.mapper

import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.data.remote.centralapp.CentralContractDto
import com.sgp.cliente.data.remote.centralapp.CentralInfoDto
import com.sgp.cliente.data.remote.centralapp.CentralInvoiceDto
import com.sgp.cliente.data.remote.centralapp.CentralMessageDto
import com.sgp.cliente.data.remote.centralapp.CentralNetworkStatusDto
import com.sgp.cliente.data.remote.centralapp.CentralPixResponseDto
import com.sgp.cliente.data.remote.centralapp.CentralServiceDto
import com.sgp.cliente.data.remote.centralapp.CentralStatementDto
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.model.SecondCopy
import com.sgp.cliente.domain.model.SecondCopyItem
import com.sgp.cliente.domain.model.ServiceStatus
import java.time.LocalDate

/** Central App DTO -> domain mappers. DTOs never cross this boundary. */

fun CentralContractDto.toDomain(): Contract {
    val internet = services.firstOrNull {
        it.group.equals("fibra", true) || it.group.equals("internet", true)
    } ?: services.firstOrNull()
    return Contract(
        id = id,
        status = status?.trim().orEmpty(),
        razaoSocial = customerName?.takeIf { it.isNotBlank() }
            ?: description.orEmpty(),
        cpfCnpj = customerDoc.orEmpty(),
        dueDay = dueDay ?: 0,
        internetPlan = internet?.plan?.takeIf { it.isNotBlank() },
        internetPlanValue = internet?.value ?: value,
        internetPlanId = internet?.id?.toString(),
    )
}

/**
 * `situacao` → SGP status ids (1 aberto, 2 pago, 3 cancelado) used by the
 * domain. Overdue bills keep status open — [Invoice.isOverdue] derives the
 * rest from the effective due date / [CentralInvoiceDto.overdueDays].
 */
fun CentralInvoiceDto.toDomain(): Invoice? {
    val statusId = when (status?.trim()?.lowercase()) {
        "cancelada", "cancelado", "cancelled" -> 3
        "paga", "pago", "liquidada", "liquidado", "paid", "quitada" -> 2
        else -> 1
    }
    val paid = paidAt?.takeIf { it.isNotBlank() }
    val total = totalAmount.toApiDouble()
    return Invoice(
        id = id,
        documentNumber = documentNumber,
        statusId = statusId,
        statusLabel = status.orEmpty(),
        dueDate = DateFormatter.parseApi(dueDate),
        effectiveDueDate = null,
        amount = amount.toApiDouble(),
        correctedAmount = total,
        paidAt = DateFormatter.parseApi(paid),
        barcode = (barcode ?: barcodeAlt)?.takeIf { it.isNotBlank() },
        pixCode = pixCode?.takeIf { it.isNotBlank() },
        canGeneratePix = pixOnDemand == true || allowsPix == true,
        // `pagamento_boleto` does not predict whether `link` serves a real
        // PDF (verified: false + valid PDF vs false + broken body) — the
        // viewer validates the payload instead of gating here.
        boletoPath = boletoUrl?.takeIf { it.isNotBlank() },
        paymentUrl = chargeUrl?.takeIf { it.isNotBlank() },
    )
}

fun CentralInvoiceDto.toSecondCopyItem(): SecondCopyItem? {
    val link = (boletoUrl ?: chargeUrl)?.takeIf { it.isNotBlank() } ?: return null
    return SecondCopyItem(
        invoiceId = id,
        barcode = (barcode ?: barcodeAlt)?.takeIf { it.isNotBlank() },
        link = link,
        amount = totalAmount.toApiDouble() ?: amount.toApiDouble(),
        dueDate = DateFormatter.parseApi(dueDate),
        originalDueDate = null,
        originalAmount = null,
    )
}

fun List<CentralInvoiceDto>.toSecondCopy(): SecondCopy {
    val items = mapNotNull { it.toSecondCopyItem() }
    return SecondCopy(
        status = 1,
        protocol = null,
        barcode = items.firstOrNull()?.barcode,
        link = items.firstOrNull()?.link,
        amount = items.firstOrNull()?.amount,
        dueDate = items.firstOrNull()?.dueDate,
        originalDueDate = null,
        message = "",
        items = items,
    )
}

fun CentralPixResponseDto.toDomain(): PixCharge = PixCharge(
    status = status ?: 0,
    message = message.orEmpty(),
    code = copyPasteCode(),
)

fun CentralMessageDto.toPaymentPromise(): PaymentPromise = PaymentPromise(
    status = status ?: 0,
    protocol = protocol?.takeIf { it.isNotBlank() },
    released = status == 1,
    message = message.orEmpty(),
)

fun CentralMessageDto.toMessageOrError(): String =
    message?.takeIf { it.isNotBlank() } ?: if (status == 1) "OK" else "Falha na operação"

fun CentralNetworkStatusDto.toDomain(): ServiceStatus = ServiceStatus(
    statusCode = status ?: 0,
    isOnline = available == true,
    message = message.orEmpty(),
)

fun CentralStatementDto.totalLabel(): String? = statement?.totalLabel

fun CentralServiceDto.isInternet(): Boolean =
    group.equals("fibra", true) ||
        group.equals("internet", true) ||
        type.equals("Internet", true)

fun CentralInfoDto.contractStatusText(): String = contractStatus?.trim().orEmpty()

fun String?.toApiDouble(): Double? =
    this?.replace(',', '.')?.toDoubleOrNull()

/** "5 dias" style overdue labels — not needed; the API sends a day count. */
fun CentralInvoiceDto.isOverdueOn(today: LocalDate): Boolean =
    (overdueDays ?: 0) > 0 ||
        (DateFormatter.parseApi(dueDate)?.isBefore(today) == true)
