package com.sgp.cliente.feature.onboarding

import com.sgp.cliente.domain.repository.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val settings = mockk<SettingsRepository>(relaxed = true) {
        every { onboardingSeen } returns flowOf(false)
    }
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnboardingViewModel(settings)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `complete persists the onboarding-seen flag`() {
        viewModel.complete()

        coVerify(exactly = 1) { settings.setOnboardingSeen() }
    }
}
