package com.sgp.cliente.update

/**
 * Ação de "Atualizar" da notificação de app-update: se a URL apontar
 * para um APK, baixa com o DownloadManager do sistema; a instalação
 * fica com o instalador do Android (um toque na tela de Downloads).
 * Qualquer outro link abre no navegador.
 */
interface UpdateInstaller {

    /** URL de APK → download; outra URL → navegador. */
    fun openUpdate(url: String, appVersion: String?)

    /** Download concluído (app reaberto ou broadcast de
     *  DOWNLOAD_COMPLETE) → abre a tela de Downloads com o APK pronto. */
    fun installIfReady(downloadId: Long? = null)
}
