package com.sgp.cliente.demo

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Interceptor

/** Dev flavor: contributes the offline demo interceptor to the OkHttp graph. */
@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    @Singleton
    @IntoSet
    fun demoModeInterceptor(impl: DemoModeInterceptor): Interceptor = impl
}
