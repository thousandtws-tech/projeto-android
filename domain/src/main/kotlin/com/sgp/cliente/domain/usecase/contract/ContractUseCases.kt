package com.sgp.cliente.domain.usecase.contract

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.onSuccess
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.repository.ContractRepository
import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject

class GetContractsUseCase @Inject constructor(
    private val contractRepository: ContractRepository,
    private val sessionRepository: SessionRepository,
) {
    /** Refreshes the contract list remotely and keeps the session copy in sync. */
    suspend operator fun invoke(): AppResult<List<Contract>> =
        contractRepository.refreshContracts()
            .onSuccess { sessionRepository.updateContracts(it) }
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
