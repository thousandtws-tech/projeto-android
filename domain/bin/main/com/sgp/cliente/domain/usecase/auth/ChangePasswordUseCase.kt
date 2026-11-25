package com.sgp.cliente.domain.usecase.auth

import com.sgp.cliente.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Changes the subscriber password via `POST /api/centralapp/alterarsenha/`.
 * On success the repository already persists the re-issued JWT and the
 * re-ciphered password, so subsequent silent re-logins use the new secret.
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
    ) = authRepository.changePassword(currentPassword, newPassword)
}
