package com.sgp.cliente

/**
 * Production flavor defaults pointing at the public SGP demo instance.
 * Distribution builds for a specific provider should set that provider's
 * public SGP base URL and its Central App (id + token listed by
 * `GET /api/centralapp/list/` — the value the provider sends as
 * `X-TOKEN` on every `/api/centralapp/` call).
 */
object EnvConfig {
    const val DEFAULT_BASE_URL: String = "https://demo.sgp.net.br"
    const val DEFAULT_CENTRAL_ID: Int = 3
    const val DEFAULT_CENTRAL_TOKEN: String = ""
}
