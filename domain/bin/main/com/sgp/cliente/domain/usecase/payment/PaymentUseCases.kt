package com.sgp.cliente.domain.usecase.payment

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.PaymentPromise
import com.sgp.cliente.domain.model.PixCharge
import com.sgp.cliente.domain.repository.PaymentRepository
import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject

class GeneratePixUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(invoiceId: Int): AppResult<PixCharge> {
        val contractId = contractIdOrNull()
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return paymentRepository.generatePix(contractId, invoiceId)
    }

    private suspend fun contractIdOrNull(): Int? =
        sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
}

/**
 * "Liberação por Confiança" / promessa de pagamento.
 * Never assumes approval: the API answer (`liberado`) is surfaced as-is.
 */
class RequestPaymentPromiseUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): AppResult<PaymentPromise> {
        val contractId = contractIdOrNull()
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return paymentRepository.requestPaymentPromise(contractId)
    }

    private suspend fun contractIdOrNull(): Int? =
        sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
}

/** Card checkout deep link (invoice `url_cobranca`/`link` from `centralapp/titulo/{id}/detail/`). */
class GetCardCheckoutUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(invoiceId: Int): AppResult<String> {
        val contractId = contractIdOrNull()
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return paymentRepository.cardCheckoutUrl(contractId, invoiceId)
    }

    private suspend fun contractIdOrNull(): Int? =
        sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
}
