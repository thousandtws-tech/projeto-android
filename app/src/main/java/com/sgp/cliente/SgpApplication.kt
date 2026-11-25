package com.sgp.cliente

import android.app.Application
import android.util.Log
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.core.notifications.NotificationChannels
import com.sgp.cliente.logging.TimberAppLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SgpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        // Canais criados cedo — qualquer push que chegue já encontra a
        // categoria certa (idempotente, custo ~zero).
        NotificationChannels.register(this)
        Timber.tag(LIFECYCLE_TAG).i(
            "app start flavor=%s buildType=%s versionName=%s",
            BuildConfig.FLAVOR,
            BuildConfig.BUILD_TYPE,
            BuildConfig.VERSION_NAME,
        )
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        AppLog.install(TimberAppLogger())
    }

    /**
     * Release tree: forwards only WARN/ERROR to Logcat. Debug/info chatter and
     * formatted message bodies never leave the process on release builds.
     */
    private class ReleaseTree : Timber.Tree() {

        override fun isLoggable(tag: String?, priority: Int): Boolean =
            priority >= Log.WARN

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val resolvedTag = tag ?: FALLBACK_TAG
            Log.println(priority, resolvedTag, message)
            if (t != null) {
                Log.println(priority, resolvedTag, Log.getStackTraceString(t))
            }
        }

        private companion object {
            const val FALLBACK_TAG = "sgp-cliente"
        }
    }

    private companion object {
        const val LIFECYCLE_TAG = "app.lifecycle"
    }
}
