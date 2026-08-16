package ch.santa.santatab

import android.content.Context
import ch.santa.santatab.data.mqtt.MqttManager
import ch.santa.santatab.data.settings.SettingsRepository
import ch.santa.santatab.domain.DiffuserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Einfacher, manueller Dependency-Container (kein Hilt nötig für den Umfang).
 * Lebt so lange wie der Prozess und hält die eine Broker-Verbindung.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(context.applicationContext)
    private val mqttManager = MqttManager()

    val diffuserRepository = DiffuserRepository(
        mqtt = mqttManager,
        settingsRepository = settingsRepository,
        scope = appScope,
    ).apply { start() }
}
