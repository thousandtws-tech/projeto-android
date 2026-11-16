package com.sgp.cliente.domain.model

/**
 * Customer contract returned by `POST /api/centralapp/clientecontrato/list/`.
 */
data class Contract(
    val id: Int,
    val status: String,
    val razaoSocial: String,
    val cpfCnpj: String,
    /** Day of month the invoices are due (`vencimento` field). */
    val dueDay: Int,
    val internetPlan: String?,
    val internetPlanValue: Double?,
    val internetPlanId: String?,
    /** Street line (`endereco_linha_0`) — how subscribers tell contracts apart. */
    val address: String?,
) {
    val isActive: Boolean
        get() = status.trim().equals(STATUS_ATIVO, ignoreCase = true)

    companion object {
        private const val STATUS_ATIVO = "Ativo"
    }
}
