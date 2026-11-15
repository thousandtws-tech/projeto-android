package com.sgp.cliente.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sgp.cliente.core.designsystem.components.SgpFloatingNavBar
import com.sgp.cliente.core.designsystem.components.SgpFloatingNavItem
import com.sgp.cliente.feature.bills.BillsScreen
import com.sgp.cliente.feature.dashboard.DashboardScreen
import com.sgp.cliente.feature.paymentpromises.PaymentPromisesScreen
import com.sgp.cliente.feature.profile.ProfileScreen

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Início", Icons.Filled.Home),
    BILLS("bills", "Faturas", Icons.AutoMirrored.Filled.ReceiptLong),
    PROMISES("promises", "Promessa", Icons.Filled.EventAvailable),
    PROFILE("profile", "Perfil", Icons.Filled.Person),
}

/**
 * Authenticated area: on compact windows the primary destinations switch
 * through [SgpFloatingNavBar] (animated floating bar with raised indicator);
 * on medium/expanded windows a rail/drawer is used instead.
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
) {
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

    if (layoutType == NavigationSuiteType.NavigationBar) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            // SgpFloatingNavBar consumes the navigation-bar inset itself.
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                SgpFloatingNavBar(
                    items = TopLevelDestination.entries.map {
                        SgpFloatingNavItem(icon = it.icon, label = it.label)
                    },
                    selectedIndex = TopLevelDestination.entries
                        .indexOfFirst { it.route == currentRoute }
                        .coerceAtLeast(0),
                    onItemSelected = { index ->
                        navigateToTab(TopLevelDestination.entries[index])
                    },
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
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
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
            MainTabsNavHost(
                tabNavController = tabNavController,
                onOpenInvoice = onOpenInvoice,
                onOpenPreferences = onOpenPreferences,
                onOpenPrivacy = onOpenPrivacy,
                onOpenContracts = onOpenContracts,
                viewModelStoreOwner = viewModelStoreOwner,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    NavHost(
        navController = tabNavController,
        startDestination = TopLevelDestination.DASHBOARD.route,
        modifier = modifier.padding(contentPadding),
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
