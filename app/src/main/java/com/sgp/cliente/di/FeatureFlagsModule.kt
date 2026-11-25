package com.sgp.cliente.di

import com.sgp.cliente.EnvConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Feature flags de build — definidas por flavor em `EnvConfig` e
 * injetadas por nome nos consumidores (features não enxergam o app).
 */
@Module
@InstallIn(SingletonComponent::class)
object FeatureFlagsModule {

    /** Regra J D FIBRA: 2ª via por e-mail/SMS gera ordem de serviço no
     * SGP — o envio fica desligado onde o provedor pediu. */
    @Provides
    @Named("sendInvoiceCopyEnabled")
    fun sendInvoiceCopyEnabled(): Boolean = EnvConfig.SEND_INVOICE_COPY_ENABLED
}
