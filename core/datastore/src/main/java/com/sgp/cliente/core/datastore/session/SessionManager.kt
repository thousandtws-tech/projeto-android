package com.sgp.cliente.core.datastore.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sgp.cliente.core.datastore.security.CredentialCipher
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.Credentials
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sgp_session",
)

/**
 * Persists the customer session in Preferences DataStore.
 * The Central API re-sends credentials on every request, so the password
 * is kept encrypted (Android Keystore AES/GCM) — never in plain text.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: CredentialCipher,
) : SessionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val store get() = context.sessionDataStore

    override val state: Flow<SessionState> = store.data.map { prefs ->
        val login = prefs[Keys.LOGIN]
        val encryptedPassword = prefs[Keys.PASSWORD_CIPHER]
        if (login == null || encryptedPassword == null) {
            SessionState.LoggedOut
        } else {
            val password = cipher.decrypt(encryptedPassword)
            if (password == null) {
                SessionState.LoggedOut
            } else {
                SessionState.LoggedIn(
                    CustomerSession(
                        credentials = Credentials(login, password),
                        customerName = prefs[Keys.CUSTOMER_NAME].orEmpty(),
                        cpfCnpj = prefs[Keys.CPF_CNPJ].orEmpty(),
                        contracts = decodeContracts(prefs[Keys.CONTRACTS_JSON]),
                        selectedContractId = prefs[Keys.SELECTED_CONTRACT],
                    ),
                )
            }
        }
    }

    override suspend fun persist(session: CustomerSession) {
        store.edit { prefs ->
            prefs[Keys.LOGIN] = session.credentials.login
            prefs[Keys.PASSWORD_CIPHER] = cipher.encrypt(session.credentials.password)
            prefs[Keys.CUSTOMER_NAME] = session.customerName
            prefs[Keys.CPF_CNPJ] = session.cpfCnpj
            prefs[Keys.CONTRACTS_JSON] = encodeContracts(session.contracts)
            session.selectedContractId?.let { prefs[Keys.SELECTED_CONTRACT] = it }
        }
    }

    override suspend fun updateContracts(contracts: List<Contract>) {
        store.edit { prefs ->
            prefs[Keys.CONTRACTS_JSON] = encodeContracts(contracts)
        }
    }

    override suspend fun selectContract(contractId: Int) {
        store.edit { prefs ->
            prefs[Keys.SELECTED_CONTRACT] = contractId
        }
    }

    override suspend fun credentials(): Credentials? =
        when (val current = state.first()) {
            is SessionState.LoggedIn -> current.session.credentials
            else -> null
        }

    override suspend fun selectedContractId(): Int? =
        store.data.first()[Keys.SELECTED_CONTRACT]

    override suspend fun selectedContract(): Contract? =
        when (val current = state.first()) {
            is SessionState.LoggedIn -> current.session.selectedContract
            else -> null
        }

    override suspend fun authToken(): String? =
        store.data.first()[Keys.JWT_TOKEN]?.takeIf(String::isNotBlank)

    override suspend fun persistAuthToken(token: String) {
        store.edit { prefs -> prefs[Keys.JWT_TOKEN] = token }
    }

    override suspend fun updatePassword(newPassword: String) {
        store.edit { prefs -> prefs[Keys.PASSWORD_CIPHER] = cipher.encrypt(newPassword) }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }

    private fun encodeContracts(contracts: List<Contract>): String =
        json.encodeToString(contracts.map(StoredContract::fromDomain))

    private fun decodeContracts(raw: String?): List<Contract> =
        raw?.let {
            runCatching {
                json.decodeFromString<List<StoredContract>>(it).map(StoredContract::toDomain)
            }.getOrDefault(emptyList())
        } ?: emptyList()

    private object Keys {
        val LOGIN = stringPreferencesKey("login")
        val CPF_CNPJ = stringPreferencesKey("cpfcnpj")
        val PASSWORD_CIPHER = stringPreferencesKey("password_cipher")
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val CUSTOMER_NAME = stringPreferencesKey("customer_name")
        val CONTRACTS_JSON = stringPreferencesKey("contracts_json")
        val SELECTED_CONTRACT = intPreferencesKey("selected_contract")
    }
}

/** Serialized form of [Contract] for DataStore persistence. */
@Serializable
private data class StoredContract(
    val id: Int,
    val status: String,
    val razaoSocial: String,
    val cpfCnpj: String,
    val dueDay: Int,
    val internetPlan: String? = null,
    val internetPlanValue: Double? = null,
    val internetPlanId: String? = null,
    val address: String? = null,
) {
    fun toDomain() = Contract(
        id = id,
        status = status,
        razaoSocial = razaoSocial,
        cpfCnpj = cpfCnpj,
        dueDay = dueDay,
        internetPlan = internetPlan,
        internetPlanValue = internetPlanValue,
        internetPlanId = internetPlanId,
        address = address,
    )

    companion object {
        fun fromDomain(contract: Contract) = StoredContract(
            id = contract.id,
            status = contract.status,
            razaoSocial = contract.razaoSocial,
            cpfCnpj = contract.cpfCnpj,
            dueDay = contract.dueDay,
            internetPlan = contract.internetPlan,
            internetPlanValue = contract.internetPlanValue,
            internetPlanId = contract.internetPlanId,
            address = contract.address,
        )
    }
}
