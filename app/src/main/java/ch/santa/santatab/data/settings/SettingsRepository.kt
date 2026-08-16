package ch.santa.santatab.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.santa.santatab.data.model.AppMode
import ch.santa.santatab.data.model.AppSettings
import ch.santa.santatab.data.model.BrokerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "santatab_settings")

/** Persistiert die App-Einstellungen (Modus, Broker, Direkt-Geräte) über Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            mode = AppMode.fromName(prefs[KEY_MODE]),
            broker = BrokerSettings(
                host = prefs[KEY_HOST] ?: "",
                port = prefs[KEY_PORT] ?: BrokerSettings.DEFAULT_PORT,
                username = prefs[KEY_USER] ?: "",
                password = prefs[KEY_PASS] ?: "",
                discoveryPrefix = prefs[KEY_PREFIX] ?: BrokerSettings.DEFAULT_DISCOVERY_PREFIX,
            ),
            directHosts = (prefs[KEY_DIRECT_HOSTS] ?: "")
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() },
        )
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MODE] = settings.mode.name
            prefs[KEY_HOST] = settings.broker.host.trim()
            prefs[KEY_PORT] = settings.broker.port.let { if (it in 1..65535) it else BrokerSettings.DEFAULT_PORT }
            prefs[KEY_USER] = settings.broker.username.trim()
            prefs[KEY_PASS] = settings.broker.password
            prefs[KEY_PREFIX] = settings.broker.discoveryPrefix.trim()
                .ifBlank { BrokerSettings.DEFAULT_DISCOVERY_PREFIX }
            prefs[KEY_DIRECT_HOSTS] = settings.directHosts
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("app_mode")
        val KEY_HOST = stringPreferencesKey("broker_host")
        val KEY_PORT = intPreferencesKey("broker_port")
        val KEY_USER = stringPreferencesKey("broker_user")
        val KEY_PASS = stringPreferencesKey("broker_pass")
        val KEY_PREFIX = stringPreferencesKey("discovery_prefix")
        val KEY_DIRECT_HOSTS = stringPreferencesKey("direct_hosts")
    }
}
