package ch.santa.santatab.data.mqtt

import ch.santa.santatab.data.model.BrokerSettings
import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.ConnectionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/** Eine eingehende MQTT-Nachricht. */
data class MqttEvent(val topic: String, val payload: String)

/**
 * Dünner Wrapper um den Paho-MQTT-Client, der Verbindungszustand und
 * eingehende Nachrichten als Kotlin-Flows bereitstellt.
 *
 * Bewusst ohne den (veralteten) Paho-Android-Service: Der reine Java-Client
 * läuft mit eigenem Thread-Pool und automatischem Reconnect.
 */
class MqttManager {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<MqttEvent>(
        replay = 0,
        extraBufferCapacity = 256,
    )
    val events: SharedFlow<MqttEvent> = _events.asSharedFlow()

    private var client: MqttAsyncClient? = null

    @Synchronized
    fun connect(settings: BrokerSettings) {
        if (!settings.isConfigured) {
            _connectionState.value = ConnectionState(ConnectionStatus.DISCONNECTED, "Kein Broker konfiguriert")
            return
        }
        // Bestehende Verbindung sauber schliessen, bevor eine neue aufgebaut wird.
        disconnect()

        _connectionState.value = ConnectionState(ConnectionStatus.CONNECTING, settings.serverUri)

        val clientId = "santatab-" + System.currentTimeMillis().toString(36)
        val mqtt = try {
            MqttAsyncClient(settings.serverUri, clientId, MemoryPersistence())
        } catch (e: MqttException) {
            _connectionState.value = ConnectionState(ConnectionStatus.ERROR, e.message ?: "Ungültige Broker-Adresse")
            return
        }

        mqtt.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                _connectionState.value = ConnectionState(ConnectionStatus.CONNECTED, serverURI)
                onConnected?.invoke(reconnect)
            }

            override fun connectionLost(cause: Throwable?) {
                _connectionState.value =
                    ConnectionState(ConnectionStatus.CONNECTING, "Verbindung verloren – neuer Versuch …")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                _events.tryEmit(MqttEvent(topic, String(message.payload, Charsets.UTF_8)))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 30
            if (settings.username.isNotBlank()) {
                userName = settings.username
                password = settings.password.toCharArray()
            }
        }

        client = mqtt
        try {
            mqtt.connect(options)
        } catch (e: MqttException) {
            _connectionState.value = ConnectionState(ConnectionStatus.ERROR, e.message ?: "Verbindung fehlgeschlagen")
        }
    }

    /** Callback, das nach erfolgreicher (Wieder-)Verbindung feuert. */
    var onConnected: ((reconnect: Boolean) -> Unit)? = null

    fun subscribe(topic: String, qos: Int = 0) {
        val c = client ?: return
        try {
            if (c.isConnected) c.subscribe(topic, qos)
        } catch (_: MqttException) {
            // Ignorieren: Bei Reconnect wird erneut abonniert.
        }
    }

    fun publish(topic: String, payload: String, qos: Int = 0, retained: Boolean = false) {
        val c = client ?: return
        try {
            if (c.isConnected) {
                c.publish(topic, payload.toByteArray(Charsets.UTF_8), qos, retained)
            }
        } catch (_: MqttException) {
            // Bei fehlender Verbindung wird der Befehl verworfen; die UI zeigt den Zustand.
        }
    }

    @Synchronized
    fun disconnect() {
        val c = client ?: return
        client = null
        try {
            c.setCallback(null)
            if (c.isConnected) c.disconnect() else c.close()
        } catch (_: MqttException) {
            runCatching { c.close(true) }
        }
        _connectionState.value = ConnectionState.Disconnected
    }
}
