package ch.santa.santatab.data.model

/**
 * Gesamte App-Konfiguration: gewählter Modus plus die jeweiligen Verbindungsdaten.
 */
data class AppSettings(
    val mode: AppMode = AppMode.MQTT,
    val broker: BrokerSettings = BrokerSettings(),
    /** Hosts/IPs der Diffuser im Direkt-Modus, z. B. "10.1.1.61" oder "duft-wohnzimmer.local". */
    val directHosts: List<String> = emptyList(),
) {
    val isConfigured: Boolean
        get() = when (mode) {
            AppMode.MQTT -> broker.isConfigured
            AppMode.DIRECT -> directHosts.isNotEmpty()
        }
}
