package com.sgp.cliente.core.network.error

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.failure(e.toAppError())
    } catch (e: SocketTimeoutException) {
        AppResult.failure(AppError.Timeout)
    } catch (e: UnknownHostException) {
        AppResult.failure(AppError.NetworkUnavailable)
    } catch (e: java.io.IOException) {
        AppResult.failure(AppError.NetworkUnavailable)
    } catch (e: SerializationException) {
        AppResult.failure(AppError.InvalidResponse)
    } catch (e: IllegalArgumentException) {
        AppResult.failure(AppError.InvalidResponse)
    } catch (e: Throwable) {
        AppResult.failure(AppError.Unknown(e))
    }

private fun HttpException.toAppError(): AppError = when (code()) {
    401, 403 -> AppError.Unauthorized
    404 -> AppError.NotFound
    400, 422 -> AppError.Validation(apiMessage = response()?.errorBody()?.safeMessage())
    in 500..599 -> AppError.Server
    else -> AppError.Unknown(this)
}

private fun okhttp3.ResponseBody?.safeMessage(): String? =
    runCatching { this?.string()?.take(300)?.takeIf { it.isNotBlank() } }.getOrNull()
