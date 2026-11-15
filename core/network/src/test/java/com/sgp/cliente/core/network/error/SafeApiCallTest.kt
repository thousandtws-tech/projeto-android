package com.sgp.cliente.core.network.error

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

/**
 * Unit tests for [safeApiCall]: every failure transport (HTTP code, socket,
 * serialization, unexpected) must map onto a typed [AppError], and coroutine
 * cancellation must be re-thrown untouched.
 */
class SafeApiCallTest {

    private fun httpException(code: Int, errorBody: String = ""): HttpException =
        HttpException(
            retrofit2.Response.error<Any>(
                code,
                errorBody.toResponseBody("text/plain".toMediaType()),
            ),
        )

    private suspend fun <T> failingCall(t: Throwable): AppResult<T> =
        safeApiCall { throw t }

    @Test
    fun `success wraps the value`() = runTest {
        val result = safeApiCall { 42 }

        assertEquals(AppResult.success(42), result)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `cancellation is rethrown not swallowed`() = runTest {
        val result = runCatching {
            safeApiCall<Unit> { throw CancellationException("cancelled") }
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `401 maps to Unauthorized`() = runTest {
        val result = failingCall<Int>(httpException(401))

        assertEquals(AppError.Unauthorized, result.errorOrNull())
    }

    @Test
    fun `403 maps to Unauthorized`() = runTest {
        val result = failingCall<Int>(httpException(403))

        assertEquals(AppError.Unauthorized, result.errorOrNull())
    }

    @Test
    fun `404 maps to NotFound`() = runTest {
        val result = failingCall<Int>(httpException(404))

        assertEquals(AppError.NotFound, result.errorOrNull())
    }

    @Test
    fun `400 maps to Validation carrying the error body`() = runTest {
        val result = failingCall<Int>(httpException(400, """{"msg":"CPF inválido"}"""))

        val error = result.errorOrNull()
        assertTrue(error is AppError.Validation)
        assertEquals("""{"msg":"CPF inválido"}""", (error as AppError.Validation).apiMessage)
    }

    @Test
    fun `422 maps to Validation`() = runTest {
        val result = failingCall<Int>(httpException(422, "campo obrigatório"))

        val error = result.errorOrNull()
        assertTrue(error is AppError.Validation)
        assertEquals("campo obrigatório", (error as AppError.Validation).apiMessage)
    }

    @Test
    fun `400 without a readable body maps to Validation with null message`() = runTest {
        val result = failingCall<Int>(httpException(400, ""))

        val error = result.errorOrNull()
        assertTrue(error is AppError.Validation)
        assertNull((error as AppError.Validation).apiMessage)
    }

    @Test
    fun `error body is truncated at 300 chars`() = runTest {
        val result = failingCall<Int>(httpException(400, "a".repeat(400)))

        val error = result.errorOrNull() as AppError.Validation
        assertEquals(300, error.apiMessage!!.length)
    }

    @Test
    fun `5xx maps to Server`() = runTest {
        assertEquals(AppError.Server, failingCall<Int>(httpException(500)).errorOrNull())
        assertEquals(AppError.Server, failingCall<Int>(httpException(503)).errorOrNull())
    }

    @Test
    fun `unexpected http code maps to Unknown keeping the cause`() = runTest {
        val result = failingCall<Int>(httpException(418))

        val error = result.errorOrNull()
        assertTrue(error is AppError.Unknown)
        assertTrue((error as AppError.Unknown).cause is HttpException)
    }

    @Test
    fun `socket timeout maps to Timeout`() = runTest {
        val result = failingCall<Int>(SocketTimeoutException("read timed out"))

        assertEquals(AppError.Timeout, result.errorOrNull())
    }

    @Test
    fun `unknown host maps to NetworkUnavailable`() = runTest {
        val result = failingCall<Int>(UnknownHostException("sgp.exemplo.com.br"))

        assertEquals(AppError.NetworkUnavailable, result.errorOrNull())
    }

    @Test
    fun `generic io failure maps to NetworkUnavailable`() = runTest {
        val result = failingCall<Int>(IOException("connection reset"))

        assertEquals(AppError.NetworkUnavailable, result.errorOrNull())
    }

    @Test
    fun `serialization failure maps to InvalidResponse`() = runTest {
        val result = failingCall<Int>(SerializationException("unexpected token"))

        assertEquals(AppError.InvalidResponse, result.errorOrNull())
    }

    @Test
    fun `illegal argument maps to InvalidResponse`() = runTest {
        val result = failingCall<Int>(IllegalArgumentException("bad url"))

        assertEquals(AppError.InvalidResponse, result.errorOrNull())
    }

    @Test
    fun `unexpected throwable maps to Unknown keeping the cause`() = runTest {
        val boom = IllegalStateException("boom")

        val result = failingCall<Int>(boom)

        val error = result.errorOrNull()
        assertTrue(error is AppError.Unknown)
        assertEquals(boom, (error as AppError.Unknown).cause)
    }
}
