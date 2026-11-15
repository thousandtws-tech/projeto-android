package com.sgp.cliente.core.network.di

import com.sgp.cliente.core.network.api.BaseUrlInterceptor
import com.sgp.cliente.core.network.api.BaseUrlProvider
import com.sgp.cliente.core.network.api.SanitizedLoggingInterceptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Wiring tests for [NetworkModule]: timeouts, interceptor ordering
 * (flavor extras first) and the placeholder Retrofit base URL that
 * [BaseUrlInterceptor] rewrites per request.
 */
class NetworkModuleTest {

    private class NoopInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response =
            chain.proceed(chain.request())
    }

    @Test
    fun `okhttp client applies the documented timeouts`() {
        val client = NetworkModule.provideOkHttpClient(
            BaseUrlInterceptor(BaseUrlProvider()),
            SanitizedLoggingInterceptor(),
            emptySet(),
        )

        assertEquals(20_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
    }

    @Test
    fun `flavor interceptors run before base url and logging`() {
        val extra = NoopInterceptor()
        val baseUrl = BaseUrlInterceptor(BaseUrlProvider())
        val logging = SanitizedLoggingInterceptor()

        val client = NetworkModule.provideOkHttpClient(baseUrl, logging, setOf(extra))

        assertEquals(listOf(extra, baseUrl, logging), client.interceptors)
    }

    @Test
    fun `retrofit uses the placeholder base url rewritten per request`() {
        val retrofit = NetworkModule.provideRetrofit(
            NetworkModule.provideOkHttpClient(
                BaseUrlInterceptor(BaseUrlProvider()),
                SanitizedLoggingInterceptor(),
                emptySet(),
            ),
            Json { ignoreUnknownKeys = true },
        )

        assertEquals("https://placeholder.local/", retrofit.baseUrl().toString())
        assertNotNull(retrofit.converterFactories().firstOrNull())
    }

    @Test
    fun `provided json tolerates lenient payloads`() {
        val json = NetworkModule.provideJson()

        // isLenient accepts unquoted literals; parseable without a schema.
        val element = json.parseToJsonElement("{flag: true}")

        assertEquals(true, element.jsonObject["flag"]!!.jsonPrimitive.boolean)
    }
}
