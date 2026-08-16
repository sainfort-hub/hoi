package ch.santa.santatab.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.santa.santatab.data.model.BrokerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "santatab_settings")

/** Persistiert die Broker-Einstellungen über Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<BrokerSettings> = context.dataStore.data.map { prefs ->
        BrokerSettings(
            host = prefs[KEY_HOST] ?: "",
            port = prefs[KEY_PORT] ?: BrokerSettings.DEFAULT_PORT,
            username = prefs[KEY_USER] ?: "",
            password = prefs[KEY_PASS] ?: "",
            discoveryPrefix = prefs[KEY_PREFIX] ?: BrokerSettings.DEFAULT_DISCOVERY_PREFIX,
        )
    }

    suspend fun save(settings: BrokerSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOST] = settings.host.trim()
            prefs[KEY_PORT] = if (settings.port in 1..65535) settings.port else BrokerSettings.DEFAULT_PORT
            prefs[KEY_USER] = settings.username.trim()
            prefs[KEY_PASS] = settings.password
            prefs[KEY_PREFIX] = settings.discoveryPrefix.trim()
                .ifBlank { BrokerSettings.DEFAULT_DISCOVERY_PREFIX }
        }
    }

    private companion object {
        val KEY_HOST = stringPreferencesKey("broker_host")
        val KEY_PORT = intPreferencesKey("broker_port")
        val KEY_USER = stringPreferencesKey("broker_user")
        val KEY_PASS = stringPreferencesKey("broker_pass")
        val KEY_PREFIX = stringPreferencesKey("discovery_prefix")
    }
}
