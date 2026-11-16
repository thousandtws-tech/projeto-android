package com.sgp.cliente.core.network.api

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

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
