package com.sgp.cliente

/**
 * Staging flavor defaults pointing at the public SGP demo instance.
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

    /** CRM backend — vazio até existir URL pública de staging. */
    const val CRM_API_URL: String = ""

    /** Slug do tenant no CRM multi-tenant — vazio = tenant platform. */
    const val CRM_TENANT: String = ""

    /** Envio de 2ª via por e-mail/SMS — segue prod (desligado). */
    const val SEND_INVOICE_COPY_ENABLED: Boolean = false
}
