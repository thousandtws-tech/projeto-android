package com.sgp.cliente.di

import com.sgp.cliente.update.UpdateDownloader
import com.sgp.cliente.update.UpdateInstaller
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UpdateModule {

    @Binds
    @Singleton
    fun bindUpdateInstaller(impl: UpdateDownloader): UpdateInstaller
}
