package ch.santa.santatab.data.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/** Zustand der Verbindung zum MQTT-Broker inkl. optionalem Klartext-Detail. */
data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val detail: String? = null,
) {
    companion object {
        val Disconnected = ConnectionState(ConnectionStatus.DISCONNECTED)
    }
}
