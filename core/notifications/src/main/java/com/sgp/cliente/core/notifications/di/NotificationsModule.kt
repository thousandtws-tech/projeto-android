package com.sgp.cliente.core.notifications.di

import com.sgp.cliente.core.notifications.cms.CmsBannerRepository
import com.sgp.cliente.core.notifications.cms.CmsBrandingRepository
import com.sgp.cliente.core.notifications.cms.CmsOnboardingRepository
import com.sgp.cliente.domain.repository.BannerRepository
import com.sgp.cliente.domain.repository.BrandingRepository
import com.sgp.cliente.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindBannerRepository(impl: CmsBannerRepository): BannerRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: CmsOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindBrandingRepository(impl: CmsBrandingRepository): BrandingRepository
}
