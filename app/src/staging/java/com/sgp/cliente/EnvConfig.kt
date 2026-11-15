package com.sgp.cliente

/**
 * Staging flavor defaults. Point these at the provider's staging SGP
 * instance and its Central App (id + token from `GET /api/centralapp/list/`)
 * before distributing the build.
 */
object EnvConfig {
    const val DEFAULT_BASE_URL: String = ""
    const val DEFAULT_CENTRAL_ID: Int = 0
    const val DEFAULT_CENTRAL_TOKEN: String = ""
}
