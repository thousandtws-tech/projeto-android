package com.sgp.cliente.domain.usecase.auth

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.common.result.onSuccess
import com.sgp.cliente.core.common.validation.CpfCnpjValidator
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.repository.AuthRepository
import com.sgp.cliente.domain.repository.SessionRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    /**
     * The Central `login` field accepts CPF, CNPJ or the subscriber's
     * central/PPPoE username (`modo_autenticacao` decides). When the input
     * is all digits and shaped like a document we normalize it; anything
     * else goes through untouched.
     */
    suspend operator fun invoke(login: String, password: String): AppResult<CustomerSession> {
        val identifier = normalizeLogin(login)
        if (identifier.isBlank()) {
            return AppResult.failure(AppError.Validation("Informe o CPF/CNPJ ou usuário"))
        }
        if (password.isBlank()) {
            return AppResult.failure(AppError.Validation("Informe a senha da central"))
        }
        return authRepository.login(identifier, password)
            .onSuccess { sessionRepository.persist(it) }
    }

    private fun normalizeLogin(raw: String): String {
        val trimmed = raw.trim()
        val digits = CpfCnpjValidator.digits(trimmed)
        return if (digits.length == CPF_LENGTH || digits.length == CNPJ_LENGTH) digits else trimmed
    }

    private companion object {
        const val CPF_LENGTH = 11
        const val CNPJ_LENGTH = 14
    }
}
