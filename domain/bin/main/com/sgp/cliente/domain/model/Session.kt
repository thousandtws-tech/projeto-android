package com.sgp.cliente.domain.model

/**
 * Authenticated session. The Central App API issues a JWT at login;
 * [credentials] are kept — encrypted at rest — only to re-authenticate
 * silently when the token expires.
 */
data class CustomerSession(
    val credentials: Credentials,
    val customerName: String,
    /** Customer document reported by the API (display/masking only). */
    val cpfCnpj: String = "",
    val contracts: List<Contract>,
    val selectedContractId: Int?,
) {

    val selectedContract: Contract?
        get() = contracts.firstOrNull { it.id == selectedContractId }
            ?: contracts.firstOrNull { it.isActive }
            ?: contracts.firstOrNull()
}

/** A Central App instance discovered on the provider's server. */
data class CentralOption(
    val id: Int,
    val name: String,
)

/** Session state observed by the app root to pick the navigation graph. */
sealed interface SessionState {
    /** Restoring persisted state / unknown yet. */
    data object Loading : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(val session: CustomerSession) : SessionState
}
