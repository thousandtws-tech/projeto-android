package com.sgp.cliente.ui

import com.sgp.cliente.domain.model.InAppNotification
import com.sgp.cliente.domain.repository.InboxRepository
import com.sgp.cliente.update.UpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeInbox : InboxRepository {
        val items = MutableStateFlow<List<InAppNotification>>(emptyList())
        val arrivalsFlow = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 8)
        var dismissModalCalls = mutableListOf<String>()
        var markAllReadCalls = 0

        override val notifications = items
        override val arrivals: SharedFlow<InAppNotification> = arrivalsFlow

        override suspend fun add(item: InAppNotification) {
            items.value = items.value.filterNot { it.id == item.id } + item
            arrivalsFlow.emit(item)
        }
        override suspend fun markRead(id: String) {
            items.value = items.value.map { if (it.id == id) it.copy(read = true) else it }
        }
        override suspend fun markAllRead() {
            markAllReadCalls++
            items.value = items.value.map { it.copy(read = true) }
        }
        override suspend fun dismissModal(id: String) {
            dismissModalCalls.add(id)
            items.value = items.value.map { if (it.id == id) it.copy(modalDismissed = true) else it }
        }
    }

    private class FakeUpdateInstaller : UpdateInstaller {
        val opened = mutableListOf<Pair<String, String?>>()

        override fun openUpdate(url: String, appVersion: String?) {
            opened += url to appVersion
        }
        override fun installIfReady(downloadId: Long?) = Unit
    }

    private fun item(
        id: String,
        type: String = "announcement",
        presentation: String = "",
        appVersion: String? = null,
        actionUrl: String? = null,
        modalDismissed: Boolean = false,
    ) = InAppNotification(
        id = id, type = type, title = "Título $id", body = "Corpo",
        receivedAt = 0L, presentation = presentation, actionUrl = actionUrl,
        appVersion = appVersion, modalDismissed = modalDismissed,
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `live arrival bumps the bell animation tick`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(item("n1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.arrivalTick.value)
        assertEquals(1, vm.unreadCount.value)
    }

    @Test
    fun `app_update arrival presents the update modal`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(item("u1", type = InAppNotification.TYPE_APP_UPDATE, appVersion = "99.0.0"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("u1", vm.modal.value?.id)
    }

    @Test
    fun `modal presentation flag presents the announcement modal`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(item("m1", presentation = InAppNotification.PRESENTATION_MODAL))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("m1", vm.modal.value?.id)
    }

    @Test
    fun `plain notification does not open a modal`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(item("p1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.modal.value)
    }

    @Test
    fun `app_update for an already-installed version is ignored`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        // Inferior à build atual → não avisar de novo.
        inbox.add(item("u2", type = InAppNotification.TYPE_APP_UPDATE, appVersion = "0.0.1"))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.modal.value)
    }

    @Test
    fun `undismissed modal from before launch shows once on init`() = runTest {
        val inbox = FakeInbox()
        inbox.items.value = listOf(
            item("old", presentation = InAppNotification.PRESENTATION_MODAL),
        )
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("old", vm.modal.value?.id)
    }

    @Test
    fun `dismissed modal never pops again on relaunch`() = runTest {
        val inbox = FakeInbox()
        inbox.items.value = listOf(
            item(
                "old",
                presentation = InAppNotification.PRESENTATION_MODAL,
                modalDismissed = true,
            ),
        )
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.modal.value)
    }

    @Test
    fun `dismissModal persists the dismissal and clears the slot`() = runTest {
        val inbox = FakeInbox()
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(item("m2", presentation = InAppNotification.PRESENTATION_MODAL))
        dispatcher.scheduler.advanceUntilIdle()
        vm.dismissModal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("m2"), inbox.dismissModalCalls)
        assertNull(vm.modal.value)
    }

    @Test
    fun `markAllRead clears the badge`() = runTest {
        val inbox = FakeInbox()
        inbox.items.value = listOf(item("a"), item("b"))
        val vm = NotificationCenterViewModel(inbox, FakeUpdateInstaller())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.unreadCount.value)
        vm.markAllRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.unreadCount.value)
        assertEquals(1, inbox.markAllReadCalls)
    }

    @Test
    fun `update action delegates url to the installer and dismisses the modal`() = runTest {
        val inbox = FakeInbox()
        val installer = FakeUpdateInstaller()
        val vm = NotificationCenterViewModel(inbox, installer)
        dispatcher.scheduler.advanceUntilIdle()

        inbox.add(
            item(
                "u3",
                type = InAppNotification.TYPE_APP_UPDATE,
                appVersion = "99.0.0",
                actionUrl = "https://cdn.exemplo.com/app-99.apk",
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.openNotificationAction(inbox.items.value.first())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("https://cdn.exemplo.com/app-99.apk" to "99.0.0"),
            installer.opened,
        )
        assertEquals(listOf("u3"), inbox.dismissModalCalls)
        assertNull(vm.modal.value)
    }
}
