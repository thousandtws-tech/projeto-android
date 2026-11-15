package com.sgp.cliente.core.network.api

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every outgoing request onto the current SGP instance
 * ([BaseUrlProvider]). API paths are absolute (`/api/centralapp/...`) so the
 * original request URL only acts as a template; scheme/host/port and any
 * base path prefix are replaced by the configured instance URL.
 */
class BaseUrlInterceptor @Inject constructor(
    private val baseUrlProvider: BaseUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val base = baseUrlProvider.current()

        val newUrl = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .build()

        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
