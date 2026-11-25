package com.sgp.cliente.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sgp.cliente.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Publica notificações locais honrando as preferências do usuário
 * (SettingsRepository) — é aqui que os toggles de fatura/chamado
 * finalmente discriminam o que chega.
 */
@Singleton
class NotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {

    /**
     * @param kind categoria vinda do payload (`invoice`/`ticket`/outros).
     * @return true se a notificação foi publicada.
     */
    suspend fun notify(
        kind: String?,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Boolean {
        val resolved = SgpNotificationKind.of(kind)
        if (!resolved.isEnabled(settings)) {
            Timber.d("notificação %s suprimida pelas preferências", resolved)
            return false
        }
        if (!hasRuntimePermission()) {
            Timber.w("POST_NOTIFICATIONS não concedida — notificação descartada")
            return false
        }

        val notification = NotificationCompat.Builder(context, resolved.channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(launchIntent(data))
            .build()

        NotificationManagerCompat.from(context)
            .notify(nextId(resolved, data), notification)
        return true
    }

    private fun hasRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Toca no app — extras do payload seguem no intent para deep link futuro. */
    private fun launchIntent(data: Map<String, String>): PendingIntent {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                data.forEach { (key, value) -> putExtra("notification.$key", value) }
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Id estável por notificação remota (evita empilhar duplicatas do mesmo aviso). */
    private fun nextId(kind: SgpNotificationKind, data: Map<String, String>): Int =
        data["notificationId"]?.hashCode()
            ?: kind.ordinal * ID_BLOCK + fallbackIds.incrementAndGet()

    private suspend fun SgpNotificationKind.isEnabled(settings: SettingsRepository): Boolean =
        when (this) {
            SgpNotificationKind.INVOICE -> settings.invoiceAlertsEnabled.first()
            SgpNotificationKind.TICKET -> settings.ticketUpdatesEnabled.first()
            SgpNotificationKind.GENERAL -> true
        }

    private companion object {
        const val ID_BLOCK = 10_000
        val fallbackIds = AtomicInteger(0)
    }
}

enum class SgpNotificationKind(val channelId: String) {
    INVOICE(NotificationChannels.INVOICES),
    TICKET(NotificationChannels.TICKETS),
    GENERAL(NotificationChannels.GENERAL);

    companion object {
        /** `announcement`/`maintenance` são os aliases que o CRM usa
         * para avisos gerais — caem no canal "Avisos" como o default. */
        fun of(raw: String?): SgpNotificationKind = when (raw) {
            "invoice" -> INVOICE
            "ticket" -> TICKET
            else -> GENERAL
        }
    }
}
