package com.sgp.cliente.core.datastore.di

import com.sgp.cliente.core.datastore.session.SessionManager
import com.sgp.cliente.core.datastore.settings.SettingsManager
import com.sgp.cliente.domain.repository.SessionRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionManager): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsManager): SettingsRepository
}
