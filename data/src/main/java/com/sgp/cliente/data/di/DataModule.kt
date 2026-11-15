package com.sgp.cliente.data.di

import com.sgp.cliente.data.remote.centralapp.CentralAppApiService
import com.sgp.cliente.data.remote.centralapp.CentralAppAuthInterceptor
import com.sgp.cliente.data.repository.AuthRepositoryImpl
import com.sgp.cliente.data.repository.ContractRepositoryImpl
import com.sgp.cliente.data.repository.InvoiceRepositoryImpl
import com.sgp.cliente.data.repository.PaymentRepositoryImpl
import com.sgp.cliente.domain.repository.AuthRepository
import com.sgp.cliente.domain.repository.ContractRepository
import com.sgp.cliente.domain.repository.InvoiceRepository
import com.sgp.cliente.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Clock
import javax.inject.Singleton
import okhttp3.Interceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindContractRepository(impl: ContractRepositoryImpl): ContractRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    companion object {
        @Provides
        @Singleton
        fun provideCentralAppApiService(retrofit: Retrofit): CentralAppApiService =
            retrofit.create(CentralAppApiService::class.java)

        /** Adds `X-TOKEN`/`Bearer` to every `/api/centralapp/` request. */
        @Provides
        @Singleton
        @IntoSet
        fun centralAppAuthInterceptor(impl: CentralAppAuthInterceptor): Interceptor = impl

        /** Overridable in tests; keeps date-dependent business rules deterministic. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
