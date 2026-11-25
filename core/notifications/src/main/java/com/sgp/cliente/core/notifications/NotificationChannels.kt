package com.sgp.cliente.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Canais de notificação do app — criados no boot (SgpApplication), idempotente.
 * Um canal por categoria para o usuário poder granular pelo sistema além dos
 * toggles in-app (SettingsRepository).
 */
object NotificationChannels {

    const val INVOICES = "invoices"
    const val TICKETS = "tickets"
    const val GENERAL = "general"

    fun register(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    INVOICES,
                    "Faturas",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alertas de vencimento e cobrança de faturas"
                },
                NotificationChannel(
                    TICKETS,
                    "Chamados",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Atualizações de chamados e suporte"
                },
                NotificationChannel(
                    GENERAL,
                    "Avisos",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Comunicados gerais do provedor"
                },
            ),
        )
    }
}
