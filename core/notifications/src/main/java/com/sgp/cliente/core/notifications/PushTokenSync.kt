package com.sgp.cliente.core.notifications

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Porta de sincronização do token push — implementada pelo provider
 * (hoje `:core:notifications-fcm`, condicional ao google-services.json).
 * O conjunto vem vazio quando nenhum provider está no build: registrar
 * vira no-op e o app compila igual sem Firebase.
 */
interface PushTokenSync {

    /**
     * Busca o token atual no provider e registra no CRM.
     * [subscriberId] = login do assinante (CPF/CNPJ/usuário) — é a chave
     * que o CRM usa pra segmentar broadcasts por assinante.
     */
    suspend fun syncNow(subscriberId: String?)
}

@Module
@InstallIn(SingletonComponent::class)
interface PushTokenSyncsModule {
    @Multibinds
    fun pushTokenSyncs(): Set<PushTokenSync>
}
