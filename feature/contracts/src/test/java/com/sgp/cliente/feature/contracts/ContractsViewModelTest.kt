package com.sgp.cliente.feature.contracts

import app.cash.turbine.test
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.contract.GetContractOpenBillCountsUseCase
import com.sgp.cliente.domain.usecase.contract.SelectContractUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContractsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeSession: ObserveSessionUseCase = mockk()
    private val selectContract: SelectContractUseCase = mockk()
    private val getOpenBillCounts: GetContractOpenBillCountsUseCase = mockk()
    private lateinit var viewModel: ContractsViewModel

    private val contract1 = contract(id = 1)
    private val contract2 = contract(id = 2)

    private fun contract(id: Int) = Contract(
        id = id,
        status = "Ativo",
        razaoSocial = "Maria $id",
        cpfCnpj = "52998224725",
        dueDay = 10,
        internetPlan = null,
        internetPlanValue = null,
        internetPlanId = null,
    )

    private fun session(selectedContractId: Int?) = CustomerSession(
        credentials = Credentials("52998224725", "secret"),
        customerName = "Maria",
        contracts = listOf(contract1, contract2),
        selectedContractId = selectedContractId,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { selectContract(any()) } returns Unit
        coEvery { getOpenBillCounts(any()) } returns mapOf(1 to 2, 2 to 1)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `logged-in session exposes the contract list and the selected id`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedIn(session(1)))

        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(listOf(contract1, contract2), state.contracts)
        assertEquals(1, state.selectedContractId)
    }

    @Test
    fun `session updates refresh the exposed selection`() = runTest {
        val sessionFlow = MutableStateFlow<SessionState>(SessionState.LoggedIn(session(1)))
        every { observeSession() } returns sessionFlow
        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedContractId)

        sessionFlow.value = SessionState.LoggedIn(session(2))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.selectedContractId)
    }

    @Test
    fun `logged-out state keeps the screen in its initial loading state`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedOut)

        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.loading)
        assertTrue(state.contracts.isEmpty())
        assertNull(state.selectedContractId)
    }

    @Test
    fun `select delegates to the use case and emits the navigation event`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedIn(session(1)))
        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            // The SharedFlow has no replay: let the collector subscribe
            // before the emission runs on the main dispatcher.
            yield()
            viewModel.select(2)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(ContractsEvent.ContractSelected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { selectContract(2) }
    }

    @Test
    fun `contracts without open boletos are hidden after probing`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedIn(session(1)))
        coEvery { getOpenBillCounts(any()) } returns mapOf(1 to 2, 2 to 0)
        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(contract1), state.visibleContracts)
        assertEquals(1, state.hiddenCount)
        assertFalse(state.probing)
        coVerify(exactly = 1) { getOpenBillCounts(listOf(contract1, contract2)) }
    }

    @Test
    fun `a failed probe keeps the contract visible and showAll reveals hidden ones`() = runTest {
        every { observeSession() } returns flowOf(SessionState.LoggedIn(session(1)))
        coEvery { getOpenBillCounts(any()) } returns mapOf(1 to 0, 2 to null)
        viewModel = ContractsViewModel(observeSession, selectContract, getOpenBillCounts)
        dispatcher.scheduler.advanceUntilIdle()

        // Probe failed for contract2 (null) — keep it rather than hiding it.
        assertEquals(listOf(contract2), viewModel.uiState.value.visibleContracts)

        viewModel.toggleShowAll()
        assertEquals(listOf(contract1, contract2), viewModel.uiState.value.visibleContracts)
    }
}
