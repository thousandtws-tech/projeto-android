package com.sgp.cliente.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sgp.cliente.MainViewModel
import com.sgp.cliente.R
import com.sgp.cliente.core.designsystem.components.SgpLoadingView
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.feature.auth.LoginScreen
import com.sgp.cliente.feature.billdetails.BillDetailsScreen
import com.sgp.cliente.feature.contracts.ContractsScreen
import com.sgp.cliente.feature.onboarding.OnboardingScreen
import com.sgp.cliente.feature.profile.PreferencesScreen
import com.sgp.cliente.feature.profile.PrivacyPolicyScreen
import com.sgp.cliente.feature.profile.ServerConfigScreen

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val SERVER_CONFIG = "server_config"
    const val CONTRACTS = "contracts"
    const val MAIN = "main"
    const val PRIVACY = "privacy"
    const val PREFERENCES = "preferences"
    const val BILL_DETAILS = "bill_details/{invoiceId}"
    fun billDetails(invoiceId: Int) = "bill_details/$invoiceId"
}

@Composable
fun SgpApp(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val onboardingSeen by viewModel.onboardingSeen.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState, onboardingSeen) {
        when (sessionState) {
            SessionState.LoggedOut -> navController.navigate(
                if (onboardingSeen) Routes.LOGIN else Routes.ONBOARDING,
            ) {
                popUpTo(0) { inclusive = true }
            }
            is SessionState.LoggedIn ->
                if (navController.currentDestination?.route == Routes.LOGIN) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            SessionState.Loading -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(tween(220, easing = LinearOutSlowInEasing)) +
                slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 12 }
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220, easing = LinearOutSlowInEasing)) },
        popExitTransition = {
            fadeOut(tween(160)) +
                slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 12 }
        },
    ) {
        composable(Routes.LOGIN) {
            when (sessionState) {
                SessionState.Loading -> SgpLoadingView(Modifier.fillMaxSize())
                else -> LoginScreen(
                    onConfigureServer = { navController.navigate(Routes.SERVER_CONFIG) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                brandName = stringResource(R.string.app_name),
                onFinish = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SERVER_CONFIG) {
            ServerConfigScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PREFERENCES) {
            PreferencesScreen(
                onBack = { navController.popBackStack() },
                onConfigureServer = { navController.navigate(Routes.SERVER_CONFIG) },
            )
        }

        composable(Routes.CONTRACTS) {
            ContractsScreen(
                onContractSelected = { navController.popBackStack() },
            )
        }

        composable(Routes.MAIN) { entry ->
            MainTabs(
                onOpenInvoice = { id -> navController.navigate(Routes.billDetails(id)) },
                onOpenPreferences = { navController.navigate(Routes.PREFERENCES) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenContracts = { navController.navigate(Routes.CONTRACTS) },
                viewModelStoreOwner = entry,
            )
        }

        composable(
            route = Routes.BILL_DETAILS,
            arguments = listOf(navArgument("invoiceId") { type = NavType.IntType }),
        ) {
            BillDetailsScreen(onBack = { navController.popBackStack() })
        }
    }
}
