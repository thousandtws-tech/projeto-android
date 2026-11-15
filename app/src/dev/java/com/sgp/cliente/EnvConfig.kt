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
}
