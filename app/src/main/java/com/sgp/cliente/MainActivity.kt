package com.sgp.cliente

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sgp.cliente.core.designsystem.components.LocalBrandLogoUrl
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.notifications.PushRegistrationManager
import com.sgp.cliente.domain.usecase.branding.ObserveBrandingUseCase
import com.sgp.cliente.domain.usecase.branding.RefreshBrandingUseCase
import com.sgp.cliente.ui.NotificationDeepLink
import com.sgp.cliente.ui.SgpApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pushRegistration: PushRegistrationManager

    @Inject
    lateinit var observeBranding: ObserveBrandingUseCase

    @Inject
    lateinit var refreshBranding: RefreshBrandingUseCase

    /** Deep link pendente vindo do toque numa notificação — estado
     * porque o intent pode chegar com a activity já viva (onNewIntent). */
    private val deepLink = mutableStateOf<NotificationDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink.value = intent.deepLink()
        // Mantém o token FCM registrado no CRM amarrado ao assinante
        // logado (login, troca de conta, re-registro na rotação).
        pushRegistration.start(lifecycleScope)
        // Branding do tenant (logo + cores do CRM) — best-effort: falha
        // mantém o cache/identidade embutida; o push `cms_refresh`
        // rebusca quando a empresa edita no painel.
        lifecycleScope.launch { refreshBranding() }
        setContent {
            val branding by observeBranding().collectAsStateWithLifecycle()
            SgpTheme(
                brandPrimary = branding.primaryColor,
                brandAccent = branding.accentColor,
            ) {
                CompositionLocalProvider(LocalBrandLogoUrl provides branding.logoUrl) {
                    SgpApp(deepLink = deepLink.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.deepLink()?.let { deepLink.value = it }
    }

    private fun Intent.deepLink(): NotificationDeepLink? {
        val type = getStringExtra("notification.type") ?: return null
        return NotificationDeepLink(
            type = type,
            id = getStringExtra("notification.notificationId"),
        )
    }
}
