package com.sgp.cliente.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sgp.cliente.core.designsystem.components.BrandLogo
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.feature.bills.BillsScreen
import com.sgp.cliente.feature.dashboard.DashboardScreen
import com.sgp.cliente.feature.paymentpromises.PaymentPromisesScreen
import com.sgp.cliente.feature.profile.ProfileScreen

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val title: String?,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Início", null, Icons.Filled.Home),
    BILLS("bills", "Faturas", "Faturas pagas", Icons.AutoMirrored.Filled.ReceiptLong),
    PROMISES("promises", "Promessa", "Promessa de pagamento", Icons.Filled.EventAvailable),
    PROFILE("profile", "Perfil", "Minha conta", Icons.Filled.Person),
}

/**
 * Authenticated area: on compact windows the primary destinations switch
 * through a Material 3 [NavigationBar]; on medium/expanded windows a
 * rail/drawer is used instead.
 */
@Composable
fun MainTabs(
    onOpenInvoice: (Int) -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenContracts: () -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner,
    modifier: Modifier = Modifier,
    tabNavController: NavHostController = rememberNavController(),
    pendingNotification: NotificationDeepLink? = null,
) {
    RequestNotificationPermissionEffect()

    val context = LocalContext.current
    val centerViewModel: NotificationCenterViewModel = hiltViewModel(viewModelStoreOwner)
    val unreadCount by centerViewModel.unreadCount.collectAsStateWithLifecycle()
    val arrivalTick by centerViewModel.arrivalTick.collectAsStateWithLifecycle()
    val centerOpen by centerViewModel.centerOpen.collectAsStateWithLifecycle()
    val centerItems by centerViewModel.notifications.collectAsStateWithLifecycle()
    val centerModal by centerViewModel.modal.collectAsStateWithLifecycle()

    // Toque num lembrete de fatura → cai direto na aba Faturas; andere
    // types (aviso, app-update) openen de notification center. Chave no
    // id: um aviso novo re-dispara, re-tocar o mesmo não duplica.
    LaunchedEffect(pendingNotification?.id) {
        when (pendingNotification?.type) {
            null -> Unit
            "invoice" -> tabNavController.navigate(TopLevelDestination.BILLS.route) {
                launchSingleTop = true
            }
            else -> centerViewModel.openCenter()
        }
    }

    // Instalação de update: ao concluir o download do APK o instalador é
    // aberto; se o app morreu no meio, o ON_RESUME retoma o pendente.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id >= 0) centerViewModel.installFinishedUpdate(id)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                centerViewModel.installPendingUpdateIfReady()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val layoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())

    val navigateToTab: (TopLevelDestination) -> Unit = { destination ->
        if (currentRoute != destination.route) {
            // No saveState/restoreState: Navigation keeps every saved
            // NavBackStackEntryState in backStackMap and repeated tab switches
            // grow it without bound (measured ~1MB heap + ~10MB PSS per pair
            // of switches until OOM). Dropping state restores keeps memory
            // flat; the repository TTL cache still makes revisits instant.
            tabNavController.navigate(destination.route) {
                popUpTo(tabNavController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    val currentDestination =
        TopLevelDestination.entries.firstOrNull { it.route == currentRoute }
            ?: TopLevelDestination.DASHBOARD
    val topBarTitle = currentDestination.title.orEmpty()

    if (layoutType == NavigationSuiteType.NavigationBar) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                NotificationTopBar(
                    title = topBarTitle,
                    unreadCount = unreadCount,
                    arrivalTick = arrivalTick,
                    onBellClick = centerViewModel::openCenter,
                )
            },
            bottomBar = {
                MainBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = navigateToTab,
                )
            },
        ) { innerPadding ->
            MainTabsNavHost(
                tabNavController = tabNavController,
                onOpenInvoice = onOpenInvoice,
                onOpenPreferences = onOpenPreferences,
                onOpenPrivacy = onOpenPrivacy,
                onOpenContracts = onOpenContracts,
                viewModelStoreOwner = viewModelStoreOwner,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    item(
                        icon = {
                            Icon(destination.icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) },
                        selected = currentRoute == destination.route,
                        onClick = { navigateToTab(destination) },
                    )
                }
            },
            modifier = modifier.fillMaxSize(),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    NotificationTopBar(
                        title = topBarTitle,
                        unreadCount = unreadCount,
                        arrivalTick = arrivalTick,
                        onBellClick = centerViewModel::openCenter,
                    )
                },
            ) { innerPadding ->
                MainTabsNavHost(
                    tabNavController = tabNavController,
                    onOpenInvoice = onOpenInvoice,
                    onOpenPreferences = onOpenPreferences,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenContracts = onOpenContracts,
                    viewModelStoreOwner = viewModelStoreOwner,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }
    }

    if (centerOpen) {
        NotificationCenterSheet(
            items = centerItems,
            onDismiss = centerViewModel::closeCenter,
            onItemClick = { item ->
                centerViewModel.markRead(item.id)
                if (item.type == "invoice") {
                    centerViewModel.closeCenter()
                    item.id.toIntOrNull()?.let(onOpenInvoice)
                        ?: tabNavController.navigate(TopLevelDestination.BILLS.route) {
                            launchSingleTop = true
                        }
                }
            },
            onMarkAllRead = centerViewModel::markAllRead,
        )
    }

    centerModal?.let { item ->
        NotificationModal(
            item = item,
            onDismiss = centerViewModel::dismissModal,
            onAction = centerViewModel::openNotificationAction,
        )
    }
}

/** Top app bar M3 padrão: logo J D FIBRA na home (ou título da aba ativa) + sino como action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(
    title: String,
    unreadCount: Int,
    arrivalTick: Int,
    onBellClick: () -> Unit,
) {
    TopAppBar(
        title = {
            if (title.isEmpty()) {
                BrandLogo(
                    contentDescription = "Logo do provedor",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(48.dp),
                )
            } else {
                Text(title)
            }
        },
        actions = {
            NotificationBell(
                unreadCount = unreadCount,
                arrivalTick = arrivalTick,
                onClick = onBellClick,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationTopBarPreview() {
    SgpTheme {
        NotificationTopBar(
            title = "",
            unreadCount = 3,
            arrivalTick = 0,
            onBellClick = {},
        )
    }
}

@Composable
private fun MainBottomBar(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        indicatorColor = MaterialTheme.colorScheme.primary,
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(destination.icon, contentDescription = destination.label)
                },
                label = { Text(destination.label) },
                colors = itemColors,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainNavBarPreview() {
    SgpTheme {
        MainBottomBar(
            currentRoute = TopLevelDestination.DASHBOARD.route,
            onNavigate = {},
        )
    }
}

@Composable
private fun MainTabsNavHost(
    tabNavController: NavHostController,
    onOpenInvoice: (Int) -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenContracts: () -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = tabNavController,
        startDestination = TopLevelDestination.DASHBOARD.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(200, easing = LinearOutSlowInEasing)) +
                scaleIn(tween(200, easing = LinearOutSlowInEasing), initialScale = 0.98f)
        },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = {
            fadeIn(tween(200, easing = LinearOutSlowInEasing)) +
                scaleIn(tween(200, easing = LinearOutSlowInEasing), initialScale = 0.98f)
        },
        popExitTransition = { fadeOut(tween(140)) },
    ) {
        // Tab ViewModels are scoped to the MAIN graph entry so they survive
        // tab switches without saveState — bounded to four instances, each
        // holding its already-loaded data for instant revisits.
        composable(TopLevelDestination.DASHBOARD.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                DashboardScreen(
                    onOpenInvoice = onOpenInvoice,
                    onOpenBills = {
                        tabNavController.navigate(TopLevelDestination.BILLS.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenPromise = {
                        tabNavController.navigate(TopLevelDestination.PROMISES.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenContracts = onOpenContracts,
                    onOpenProfile = {
                        tabNavController.navigate(TopLevelDestination.PROFILE.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
        composable(TopLevelDestination.BILLS.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                BillsScreen(onOpenInvoice = onOpenInvoice)
            }
        }
        composable(TopLevelDestination.PROMISES.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                PaymentPromisesScreen()
            }
        }
        composable(TopLevelDestination.PROFILE.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                ProfileScreen(
                    onOpenPreferences = onOpenPreferences,
                    onOpenPrivacy = onOpenPrivacy,
                )
            }
        }
    }
}
