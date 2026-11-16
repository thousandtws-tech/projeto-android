package com.sgp.cliente.core.network.api

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class BaseUrlProvider @Inject constructor() {

    @Volatile
    private var current: HttpUrl = "https://invalid.local/".toHttpUrl()

    fun current(): HttpUrl = current

    fun update(rawUrl: String?) {
        val normalized = normalize(rawUrl) ?: return
        current = normalized
    }

    companion object {
        /** Normalizes user/ENV input into a parseable [HttpUrl], or null when invalid. */
        fun normalize(rawUrl: String?): HttpUrl? {
            val trimmed = rawUrl?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            val withScheme =
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
                else "https://$trimmed"
            val withSlash = if (withScheme.endsWith("/")) withScheme else "$withScheme/"
            return withSlash.toHttpUrlOrNull()
        }
    }
}
