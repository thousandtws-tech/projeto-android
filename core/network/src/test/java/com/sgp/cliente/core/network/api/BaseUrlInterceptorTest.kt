package com.sgp.cliente.core.network.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [BaseUrlInterceptor] using a mocked [Interceptor.Chain]:
 * verifies that scheme/host/port are rewritten onto the configured base URL
 * while path, query, method, headers and body survive untouched.
 */
class BaseUrlInterceptorTest {

    private val provider = BaseUrlProvider()
    private val interceptor = BaseUrlInterceptor(provider)

    private fun fakeResponse(request: Request, code: Int = 200): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .build()

    /** Chain mock that records the request handed to `proceed`. */
    private fun recordingChain(
        original: Request,
        forwarded: MutableList<Request>,
        code: Int = 200,
    ): Interceptor.Chain = mockk {
        every { request() } returns original
        every { proceed(any()) } answers {
            val sent = firstArg<Request>()
            forwarded += sent
            fakeResponse(sent, code)
        }
    }

    @Test
    fun `rewrites scheme host and port onto the configured base`() {
        provider.update("http://10.0.2.2:8080")
        val original = Request.Builder()
            .url("https://placeholder.local/api/centralapp/list/?pagina=1")
            .build()
        val forwarded = mutableListOf<Request>()

        interceptor.intercept(recordingChain(original, forwarded))

        val sent = forwarded.single()
        assertEquals("http", sent.url.scheme)
        assertEquals("10.0.2.2", sent.url.host)
        assertEquals(8080, sent.url.port)
    }

    @Test
    fun `keeps the original path and query verbatim`() {
        provider.update("https://sgp.exemplo.com.br")
        val original = Request.Builder()
            .url("https://placeholder.local/api/centralapp/titulo/77/pix/?contrato=566")
            .build()
        val forwarded = mutableListOf<Request>()

        interceptor.intercept(recordingChain(original, forwarded))

        val sent = forwarded.single()
        assertEquals("/api/centralapp/titulo/77/pix/", sent.url.encodedPath)
        assertEquals("contrato=566", sent.url.encodedQuery)
    }

    @Test
    fun `a path prefix on the base url does not leak into requests`() {
        // Only scheme/host/port are copied from the base — API paths are absolute.
        provider.update("https://sgp.exemplo.com.br/prefixo/")
        val original = Request.Builder()
            .url("https://placeholder.local/api/centralapp/list/")
            .build()
        val forwarded = mutableListOf<Request>()

        interceptor.intercept(recordingChain(original, forwarded))

        assertEquals(
            "https://sgp.exemplo.com.br/api/centralapp/list/",
            forwarded.single().url.toString(),
        )
    }

    @Test
    fun `method headers and body are preserved on the rewritten request`() {
        provider.update("https://sgp.exemplo.com.br")
        val original = Request.Builder()
            .url("https://placeholder.local/api/centralapp/login/")
            .header("X-TOKEN", "central-token")
            .post("login=u".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val forwarded = mutableListOf<Request>()

        interceptor.intercept(recordingChain(original, forwarded))

        val sent = forwarded.single()
        assertEquals("POST", sent.method)
        assertEquals("central-token", sent.header("X-TOKEN"))
        val body = okio.Buffer().also { sent.body!!.writeTo(it) }
        assertEquals("login=u", body.readUtf8())
    }

    @Test
    fun `returns the downstream response untouched`() {
        provider.update("https://sgp.exemplo.com.br")
        val original = Request.Builder().url("https://placeholder.local/ping").build()
        val expected = Response.Builder()
            .request(original)
            .protocol(Protocol.HTTP_1_1)
            .code(204)
            .message("No Content")
            .build()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns original
            every { proceed(any()) } returns expected
        }

        val response = interceptor.intercept(chain)

        assertSame(expected, response)
        verify(exactly = 1) { chain.proceed(any()) }
    }
}
