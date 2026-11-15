package com.sgp.cliente.domain.usecase.auth

import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() = sessionRepository.clear()
}
