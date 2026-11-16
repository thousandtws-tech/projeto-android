package com.sgp.cliente.domain.usecase.contract

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.common.result.onSuccess
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.InvoiceStatus
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.repository.ContractRepository
import com.sgp.cliente.domain.repository.InvoiceRepository
import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetContractsUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val sessionRepository: SessionRepository,
) {
    /** Refreshes the contract list remotely and keeps the session copy in sync. */
    suspend operator fun invoke(): AppResult<List<Contract>> =
        contractRepository.refreshContracts()
            .onSuccess { sessionRepository.updateContracts(it) }
}

/**
 * Open-invoice count per contract for the contract picker: only contracts
 * with an open/generated boleto (`situacao` aberta/atrasada, filtered
 * client-side — the server's `grupo=abertos` is only the payable-now
 * subset) should be offered for payment. Probes run in parallel; a failed
 * probe maps to `null` so the caller keeps the contract visible rather
 * than hiding it on a transient error.
 */
class GetContractOpenBillCountsUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
) {
    suspend operator fun invoke(contracts: List<Contract>): Map<Int, Int?> =
        coroutineScope {
            contracts.map { contract ->
                async {
                    contract.id to invoiceRepository
                        .getInvoices(
                            contractId = contract.id,
                            statusFilter = InvoiceStatus.OPEN.apiId,
                            limit = PROBE_LIMIT,
                        )
                        .fold(onSuccess = { it.items.size }, onFailure = { null })
                }
            }.awaitAll().toMap()
        }

    private companion object {
        /** First-page window — a contract with open titulos beyond it is
         * unrealistic for the picker use case. */
        const val PROBE_LIMIT = 250
    }
}

class SelectContractUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(contractId: Int) =
        sessionRepository.selectContract(contractId)
}

class CheckServiceStatusUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): AppResult<ServiceStatus> {
        val contractId = sessionRepository.selectedContractId()
            ?: sessionRepository.selectedContract()?.id
            ?: return AppResult.failure(AppError.Validation("Nenhum contrato selecionado"))
        return contractRepository.checkServiceStatus(contractId)
    }
}
