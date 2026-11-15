package com.sgp.cliente.feature.profile

import com.sgp.cliente.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk {
        every { invoiceAlertsEnabled } returns flowOf(false)
        every { ticketUpdatesEnabled } returns flowOf(true)
    }
    private lateinit var viewModel: PreferencesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { settingsRepository.setNotificationPrefs(any(), any()) } returns Unit
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `stored preferences are exposed once loaded`() = runTest {
        viewModel = PreferencesViewModel(settingsRepository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertFalse(state.invoiceAlerts)
        assertTrue(state.ticketUpdates)
        assertFalse(state.saving)
        assertFalse(state.saved)
    }

    @Test
    fun `toggles update the pending state`() = runTest {
        viewModel = PreferencesViewModel(settingsRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setInvoiceAlerts(true)
        viewModel.setTicketUpdates(false)

        val state = viewModel.uiState.value
        assertTrue(state.invoiceAlerts)
        assertFalse(state.ticketUpdates)
    }

    @Test
    fun `save persists the current toggles and marks the state as saved`() = runTest {
        viewModel = PreferencesViewModel(settingsRepository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.setInvoiceAlerts(true)
        viewModel.setTicketUpdates(false)

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setNotificationPrefs(true, false) }
        val state = viewModel.uiState.value
        assertFalse(state.saving)
        assertTrue(state.saved)
    }

    @Test
    fun `a second save while one is in flight is ignored`() = runTest {
        viewModel = PreferencesViewModel(settingsRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        // saving=true is applied synchronously — this call returns early.
        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setNotificationPrefs(any(), any()) }
        assertTrue(viewModel.uiState.value.saved)
    }
}
