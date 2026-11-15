package com.sgp.cliente.core.common.logging

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * Backend installed by the `:app` module at startup (Timber in production).
 * Any module may log through [AppLog] without depending on an Android logger.
 */
fun interface AppLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

/**
 * Process-wide structured log sink for modules that stay pure Kotlin
 * (`:core:common`, `:domain`, feature ViewModels). Defaults to a no-op so
 * unit tests never touch `android.util.Log`.
 *
 * Never log secrets: no passwords, tokens, CPF/CNPJ, PIX keys or barcodes —
 * only typed errors and state transitions.
 */
object AppLog {

    @Volatile
    private var logger: AppLogger = AppLogger { _, _, _, _ -> }

    fun install(appLogger: AppLogger) {
        logger = appLogger
    }

    fun uninstall() {
        logger = AppLogger { _, _, _, _ -> }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        logger.log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        logger.log(LogLevel.INFO, tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logger.log(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger.log(LogLevel.ERROR, tag, message, throwable)
    }
}
