package ch.santa.santatab

import android.content.Context
import ch.santa.santatab.data.mqtt.MqttManager
import ch.santa.santatab.data.settings.SettingsRepository
import ch.santa.santatab.domain.DiffuserRepository
import ch.santa.santatab.domain.DirectTransport
import ch.santa.santatab.domain.MqttTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Einfacher, manueller Dependency-Container (kein Hilt nötig für den Umfang).
 * Lebt so lange wie der Prozess und hält die aktive Verbindung.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(context.applicationContext)

    private val mqttManager = MqttManager()

    // readTimeout 0 = kein Timeout, damit der SSE-Stream offen bleibt.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val mqttTransport = MqttTransport(mqttManager, appScope)
    private val directTransport = DirectTransport(httpClient, appScope)

    val diffuserRepository = DiffuserRepository(
        mqtt = mqttTransport,
        direct = directTransport,
        settingsRepository = settingsRepository,
        scope = appScope,
    ).apply { start() }
}
