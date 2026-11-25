package com.sgp.cliente.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Download de APK via DownloadManager: o sistema cuida de rede, retry
 * e da notificação de progresso; ao concluir, o usuário instala pela
 * tela de Downloads (um toque → instalador do Android).
 *
 * Sem `REQUEST_INSTALL_PACKAGES` de propósito: um app que baixa e
 * instala APK programaticamente é o padrão que o Play Protect marca
 * como "app nocivo". Deixando a instalação para o sistema, o app não
 * pede nenhuma permissão sensível.
 *
 * O id do download fica em SharedPreferences para o caso de o app ser
 * fechado durante o download — no próximo ON_RESUME abre-se a tela de
 * Downloads com o APK pronto.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateInstaller {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun openUpdate(url: String, appVersion: String?) {
        if (!isHttpUrl(url)) {
            Timber.w("URL de atualização inválida — ignorada")
            return
        }
        if (isApkUrl(url)) enqueue(url, appVersion) else openInBrowser(url)
    }

    override fun installIfReady(downloadId: Long?) {
        val id = downloadId ?: pendingDownloadId() ?: return
        if (!downloadSucceeded(id)) return
        clearPending()
        runCatching {
            context.startActivity(
                Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Timber.w(it, "falha ao abrir a tela de Downloads") }
    }

    private fun enqueue(url: String, appVersion: String?) {
        cleanStaleApks()
        val fileName = "sgp-update-${appVersion ?: "latest"}.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Atualização do app")
            .setDescription("Baixando a nova versão…")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, fileName,
            )
        runCatching { downloadManager.enqueue(request) }
            .onSuccess { id -> prefs.edit().putLong(KEY_DOWNLOAD_ID, id).apply() }
            .onFailure { Timber.w(it, "falha ao enfileirar download da atualização") }
    }

    private fun openInBrowser(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { Timber.w(it, "falha ao abrir link da atualização") }
    }

    private fun downloadSucceeded(id: Long): Boolean {
        val cursor = runCatching {
            downloadManager.query(DownloadManager.Query().setFilterById(id))
        }.getOrNull() ?: return false
        return cursor.use {
            it.moveToFirst() &&
                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                DownloadManager.STATUS_SUCCESSFUL
        }
    }

    private fun pendingDownloadId(): Long? =
        prefs.getLong(KEY_DOWNLOAD_ID, -1L).takeIf { it >= 0 }

    private fun clearPending() = prefs.edit().remove(KEY_DOWNLOAD_ID).apply()

    private fun cleanStaleApks() {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles { file -> file.name.startsWith("sgp-update") }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val PREFS = "sgp_update"
        const val KEY_DOWNLOAD_ID = "pending_download_id"
        const val APK_MIME = "application/vnd.android.package-archive"

        fun isHttpUrl(url: String) =
            url.startsWith("https://") || url.startsWith("http://")

        fun isApkUrl(url: String) =
            url.substringBefore('?').endsWith(".apk", ignoreCase = true)
    }
}
