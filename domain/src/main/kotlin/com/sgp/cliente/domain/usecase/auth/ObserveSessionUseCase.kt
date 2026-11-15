package com.sgp.cliente.domain.usecase.auth

import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<SessionState> = sessionRepository.state
}
