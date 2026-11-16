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
}
