package com.sgp.cliente.core.common.error

/**
 * Typed application errors. Every failure crossing layer boundaries
 * (data -> domain -> presentation) is expressed as one of these.
 */
sealed interface AppError {

    /** No connectivity or DNS/socket failure before reaching the server. */
    data object NetworkUnavailable : AppError

    /** Connect/read/write timeout. */
    data object Timeout : AppError

    /** Credentials rejected (HTTP 401/403 or `auth: false` payload). */
    data object Unauthorized : AppError

    /** Resource not found (HTTP 404). */
    data object NotFound : AppError

    /** Validation rejected by the server (HTTP 400/422). Carries the API message when present. */
    data class Validation(val apiMessage: String? = null) : AppError

    /** Server-side failure (HTTP 5xx). */
    data object Server : AppError

    /** Response could not be parsed into the expected contract. */
    data object InvalidResponse : AppError

    /**
     * Business-level rejection returned inside a 2xx payload
     * (e.g. SGP `status: 0` + `msg`). [apiMessage] is the server's own text.
     */
    data class Api(val apiMessage: String) : AppError

    /** Anything that does not fit the categories above. */
    data class Unknown(val cause: Throwable? = null) : AppError
}
