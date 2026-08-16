package ch.santa.santatab.data.model

/**
 * Verbindungsdaten zum lokalen MQTT-Broker (z. B. Mosquitto).
 *
 * SantaTAB steuert die HIDE-Diffuser ausschliesslich lokal über diesen Broker.
 * Die ESPHome-Geräte melden sich per MQTT-Discovery selbstständig an; die App
 * muss daher nur den Broker kennen, nicht die einzelnen Geräte.
 */
data class BrokerSettings(
    val host: String = "",
    val port: Int = DEFAULT_PORT,
    val username: String = "",
    val password: String = "",
    val discoveryPrefix: String = DEFAULT_DISCOVERY_PREFIX,
) {
    /** Der Broker gilt als konfiguriert, sobald ein Host hinterlegt ist. */
    val isConfigured: Boolean get() = host.isNotBlank()

    val serverUri: String get() = "tcp://$host:$port"

    companion object {
        const val DEFAULT_PORT = 1883
        const val DEFAULT_DISCOVERY_PREFIX = "homeassistant"
    }
}
