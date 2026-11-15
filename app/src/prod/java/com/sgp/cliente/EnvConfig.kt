package com.sgp.cliente

/**
 * Production flavor defaults. Distribution builds for a specific provider
 * should set that provider's public SGP base URL and its Central App
 * (id + token listed by `GET /api/centralapp/list/` — the value the
 * provider sends as `X-TOKEN` on every `/api/centralapp/` call).
 * When both are blank the user configures the server in-app
 * (Profile -> Servidor do provedor), which discovers the central
 * automatically.
 */
object EnvConfig {
    const val DEFAULT_BASE_URL: String = ""
    const val DEFAULT_CENTRAL_ID: Int = 0
    const val DEFAULT_CENTRAL_TOKEN: String = ""
}
