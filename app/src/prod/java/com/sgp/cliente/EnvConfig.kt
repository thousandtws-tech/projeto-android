package com.sgp.cliente

/**
 * Production flavor defaults pointing at the provider's SGP instance
 * (J D FIBRA — Central do Assinante app "testesgp").
 *
 * The tenant's `GET /api/centralapp/list/` answers `[]` (no public
 * centrals), so first-run discovery can't pick the app — the token is
 * seeded here instead. It's the value the provider sends as `X-TOKEN`
 * on every `/api/centralapp/` call; verified live against `login/` +
 * `captcha/challenge`. The numeric id is only bookkeeping — no request
 * carries it (`config/{id}` doesn't exist on this SGP version) — so it
 * stays 0 until the provider reports the real one.
 */
object EnvConfig {
    const val DEFAULT_BASE_URL: String = "https://jdfibra.sgp.tsmx.com.br"
    const val DEFAULT_CENTRAL_ID: Int = 0
    const val DEFAULT_CENTRAL_TOKEN: String = "ade8f0ef-c27c-43aa-9a5d-6a96af1823eb"

    /**
     * Provider support WhatsApp in wa.me format (country + area + number,
     * digits only — e.g. "5573991234567"). The login screen's "esqueci o
     * acesso" link and the "Contratar" tab deep-link into this chat; leave
     * blank to hide both.
     */
    const val SUPPORT_WHATSAPP: String = "5571988529584"

    /** CRM backend em produção (Railway) — o app registra o token FCM
     * via POST /devices e recebe os disparos de /broadcasts. */
    const val CRM_API_URL: String = "https://crm-api-production-aac589ba.up.railway.app"

    /**
     * Slug do tenant no CRM multi-tenant — identifica esta build como
     * "a empresa J D FIBRA" nas rotas públicas (banners, onboarding,
     * devices). Precisa ser igual ao slug provisionado no painel
     * super-admin do CRM; slug errado → 404 e os feeds ficam vazios.
     * Vazio = tenant platform (fallback, comportamento pré-multi-tenant).
     */
    const val CRM_TENANT: String = "jd-fibra"

    /**
     * Envio de 2ª via por e-mail/SMS (`titulo/{id}/{tipo}/send/`). No SGP
     * da J D FIBRA esse endpoint abre uma ordem de serviço a cada envio —
     * desligado em produção por regra do provedor. O assinante segue com
     * a via via "Abrir boleto" (PDF), PIX e linha digitável.
     */
    const val SEND_INVOICE_COPY_ENABLED: Boolean = false
}
