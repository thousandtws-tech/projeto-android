package com.sgp.cliente.core.network.api

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SanitizedLoggingInterceptor]. `android.util.Log` is statically
 * mocked so the exact log line can be captured — the contract under test is that
 * credentials (cpfcnpj/senha/token) NEVER reach the log in clear text.
 */
class SanitizedLoggingInterceptorTest {

    private val loggedLines = mutableListOf<String>()
    private val interceptor = SanitizedLoggingInterceptor()

    private fun fakeResponse(request: Request, code: Int = 200): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .build()

    private fun chainFor(request: Request, code: Int = 200): Interceptor.Chain = mockk {
        every { request() } returns request
        every { proceed(any()) } answers { fakeResponse(firstArg(), code) }
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } answers {
            loggedLines += secondArg<String>()
            0
        }
    }

    @After
    fun tearDown() = unmockkStatic(Log::class)

    @Test
    fun `logs method path and response code`() {
        val request = Request.Builder()
            .url("https://api.test/api/centralapp/list/")
            .build()

        interceptor.intercept(chainFor(request, code = 200))

        val line = loggedLines.single()
        assertTrue(line.contains("GET"))
        assertTrue(line.contains("/api/centralapp/list/"))
        assertTrue(line.contains("200"))
    }

    @Test
    fun `never logs credentials from query headers or body`() {
        val request = Request.Builder()
            .url("https://api.test/api/centralapp/login/?cpfcnpj=12345678901&senha=top-secret")
            .header("X-TOKEN", "central-token-secret")
            .header("Authorization", "Bearer jwt-secret")
            .post(
                "login=u&senha=top-secret"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaType()),
            )
            .build()

        interceptor.intercept(chainFor(request))

        val line = loggedLines.single()
        listOf(
            "12345678901",
            "top-secret",
            "central-token-secret",
            "jwt-secret",
            "cpfcnpj",
            "senha",
        ).forEach { secret ->
            assertFalse("log leaked '$secret': $line", line.contains(secret))
        }
    }

    @Test
    fun `returns the downstream response`() {
        val request = Request.Builder().url("https://api.test/ping").build()
        val chain = chainFor(request)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertSame(request, response.request)
    }

    @Test
    fun `logs the exception class name but not its message on failure`() {
        val request = Request.Builder()
            .url("https://api.test/api/centralapp/login/?senha=top-secret")
            .build()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(any()) } throws IOException("socket reset by 10.0.0.9")
        }

        assertThrows(IOException::class.java) { interceptor.intercept(chain) }

        val line = loggedLines.single()
        assertTrue(line.contains("FAILED"))
        assertTrue(line.contains("IOException"))
        assertFalse(line.contains("socket reset"))
        assertFalse(line.contains("top-secret"))
    }
}
