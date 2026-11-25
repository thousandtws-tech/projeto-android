package com.sgp.cliente.domain.model

/**
 * Notificação do inbox do próprio app (a "central" atrás do sino).
 * Chega via FCM — `data.type` define o tipo e `presentation = "modal"`
 * abre um modal em vez de apenas listar.
 */
data class InAppNotification(
    val id: String,
    /** `type` bruto do payload FCM: invoice|ticket|announcement|app_update|… */
    val type: String,
    val title: String,
    val body: String,
    /** Epoch millis. */
    val receivedAt: Long,
    val read: Boolean = false,
    /** `"modal"` → abre um modal direto em vez de só ir para o inbox. */
    val presentation: String = "",
    /** Modal já exibido/dispensado — nunca reapresentar automaticamente. */
    val modalDismissed: Boolean = false,
    /** Destino do clique, ex.: link de download de um app-update. */
    val actionUrl: String? = null,
    /** Imagem do card no carrossel da dashboard (URL https, paisagem). */
    val imageUrl: String? = null,
    /** Número da versão quando `type == app_update`. */
    val appVersion: String? = null,
    /** `app_update` que não pode ser dispensado (só o botão "Atualizar"). */
    val mandatory: Boolean = false,
) {
    val isAppUpdate: Boolean get() = type == TYPE_APP_UPDATE
    val wantsModal: Boolean get() = isAppUpdate || presentation == PRESENTATION_MODAL

    companion object {
        const val TYPE_INVOICE = "invoice"
        const val TYPE_TICKET = "ticket"
        const val TYPE_APP_UPDATE = "app_update"
        const val PRESENTATION_MODAL = "modal"
    }
}
