package com.sgp.cliente.core.datastore.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sgp.cliente.domain.model.TenantBranding
import com.sgp.cliente.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sgp_settings",
)

/** App-level settings: SGP instance override and the selected Central App. */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val store = context.settingsDataStore

    override val baseUrlOverride: Flow<String?> =
        store.data.map { it[Keys.BASE_URL_OVERRIDE]?.takeIf(String::isNotBlank) }

    override val centralId: Flow<Int?> =
        store.data.map { it[Keys.CENTRAL_ID] }

    override val centralToken: Flow<String?> =
        store.data.map { it[Keys.CENTRAL_TOKEN]?.takeIf(String::isNotBlank) }

    override suspend fun setBaseUrlOverride(url: String?) {
        store.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(Keys.BASE_URL_OVERRIDE)
            else prefs[Keys.BASE_URL_OVERRIDE] = url.trim()
        }
    }

    override val onboardingSeen: Flow<Boolean> =
        store.data.map { it[Keys.ONBOARDING_SEEN] ?: false }

    override val invoiceAlertsEnabled: Flow<Boolean> =
        store.data.map { it[Keys.NOTIFY_INVOICE_ALERTS] ?: true }

    override val ticketUpdatesEnabled: Flow<Boolean> =
        store.data.map { it[Keys.NOTIFY_TICKET_UPDATES] ?: true }

    override val branding: Flow<TenantBranding> =
        store.data.map { prefs ->
            TenantBranding(
                name = prefs[Keys.BRAND_NAME]?.takeIf(String::isNotBlank),
                logoUrl = prefs[Keys.BRAND_LOGO_URL]?.takeIf(String::isNotBlank),
                primaryColor = prefs[Keys.BRAND_PRIMARY_COLOR]?.takeIf(String::isNotBlank),
                accentColor = prefs[Keys.BRAND_ACCENT_COLOR]?.takeIf(String::isNotBlank),
            )
        }

    override suspend fun setCentral(id: Int?, token: String?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(Keys.CENTRAL_ID) else prefs[Keys.CENTRAL_ID] = id
            if (token.isNullOrBlank()) prefs.remove(Keys.CENTRAL_TOKEN)
            else prefs[Keys.CENTRAL_TOKEN] = token.trim()
        }
    }

    override suspend fun setOnboardingSeen() {
        store.edit { it[Keys.ONBOARDING_SEEN] = true }
    }

    override suspend fun setNotificationPrefs(invoiceAlerts: Boolean, ticketUpdates: Boolean) {
        store.edit { prefs ->
            prefs[Keys.NOTIFY_INVOICE_ALERTS] = invoiceAlerts
            prefs[Keys.NOTIFY_TICKET_UPDATES] = ticketUpdates
        }
    }

    override suspend fun setBranding(branding: TenantBranding) {
        store.edit { prefs ->
            fun setOrClear(key: Preferences.Key<String>, value: String?) {
                if (value.isNullOrBlank()) prefs.remove(key) else prefs[key] = value.trim()
            }
            setOrClear(Keys.BRAND_NAME, branding.name)
            setOrClear(Keys.BRAND_LOGO_URL, branding.logoUrl)
            setOrClear(Keys.BRAND_PRIMARY_COLOR, branding.primaryColor)
            setOrClear(Keys.BRAND_ACCENT_COLOR, branding.accentColor)
        }
    }

    private object Keys {
        val BASE_URL_OVERRIDE = stringPreferencesKey("base_url_override")
        val CENTRAL_ID = intPreferencesKey("central_id")
        val CENTRAL_TOKEN = stringPreferencesKey("central_token")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val NOTIFY_INVOICE_ALERTS = booleanPreferencesKey("notify_invoice_alerts")
        val NOTIFY_TICKET_UPDATES = booleanPreferencesKey("notify_ticket_updates")
        val BRAND_NAME = stringPreferencesKey("brand_name")
        val BRAND_LOGO_URL = stringPreferencesKey("brand_logo_url")
        val BRAND_PRIMARY_COLOR = stringPreferencesKey("brand_primary_color")
        val BRAND_ACCENT_COLOR = stringPreferencesKey("brand_accent_color")
    }
}
