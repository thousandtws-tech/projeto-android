package com.sgp.cliente.di

import com.sgp.cliente.EnvConfig
import com.sgp.cliente.core.notifications.NotificationsConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationsModule {

    @Provides
    @Singleton
    fun provideNotificationsConfig(): NotificationsConfig =
        object : NotificationsConfig {
            override val crmApiUrl: String? =
                EnvConfig.CRM_API_URL.takeIf(String::isNotBlank)
            override val crmTenantSlug: String? =
                EnvConfig.CRM_TENANT.takeIf(String::isNotBlank)
        }
}
