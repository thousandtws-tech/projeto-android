package com.sgp.cliente.core.network.api

import android.util.Log
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class SanitizedLoggingInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        val response = runCatching { chain.proceed(request) }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

        response.onSuccess {
            Log.d(TAG, "${request.method} ${request.url.encodedPath} -> ${it.code} (${elapsedMs}ms)")
        }.onFailure {
            Log.d(TAG, "${request.method} ${request.url.encodedPath} -> FAILED (${elapsedMs}ms): ${it.javaClass.simpleName}")
        }
        return response.getOrThrow()
    }

    private companion object {
        const val TAG = "SgpHttp"
    }
}
