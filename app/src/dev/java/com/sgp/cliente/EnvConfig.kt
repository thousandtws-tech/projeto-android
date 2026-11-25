package com.sgp.cliente

/**
 * Development flavor defaults pointing at the public SGP demo instance.
 * The Central App (id + X-TOKEN) is seeded into settings on first run —
 * that token authenticates every `/api/centralapp/` call. The server URL
 * can still be overridden in-app via Profile -> Servidor do provedor,
 * which re-discovers the centrals exposed by the new host.
 */
object EnvConfig {
    const val DEFAULT_BASE_URL: String = "https://demo.sgp.net.br"
    const val DEFAULT_CENTRAL_ID: Int = 3
    const val DEFAULT_CENTRAL_TOKEN: String = ""
    const val SUPPORT_WHATSAPP: String = ""

    /**
     * CRM backend (services/api do monorepo) — onde o app registra o
     * token FCM via POST /devices. IP da máquina de dev na LAN para o
     * aparelho alcançar; ajuste se o IP mudar. Vazio desativa o registro.
     */
    const val CRM_API_URL: String = "http://192.168.1.8:8080"

    /**
     * Slug do tenant no CRM multi-tenant (header X-Tenant). Vazio em
     * dev: o backend cai no tenant platform — o demo compartilhado.
     * Para testar um tenant real, provisione no CRM e ponha o slug aqui.
     */
    const val CRM_TENANT: String = ""

    /** Envio de 2ª via por e-mail/SMS — ligado no demo para exercitar
     * o fluxo (em prod fica desligado: o SGP abre OS a cada envio). */
    const val SEND_INVOICE_COPY_ENABLED: Boolean = true
}
