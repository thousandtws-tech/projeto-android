package com.sgp.cliente.logging

import com.sgp.cliente.core.common.logging.AppLogger
import com.sgp.cliente.core.common.logging.LogLevel
import timber.log.Timber

/** Bridges the pure-Kotlin `AppLog` facade (core:common) onto Timber. */
class TimberAppLogger : AppLogger {

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val tree = Timber.tag(tag)
        when (level) {
            LogLevel.DEBUG -> tree.d(throwable, message)
            LogLevel.INFO -> tree.i(throwable, message)
            LogLevel.WARN -> tree.w(throwable, message)
            LogLevel.ERROR -> tree.e(throwable, message)
        }
    }
}
