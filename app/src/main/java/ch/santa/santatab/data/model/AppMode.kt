package ch.santa.santatab.data.model

/** Steuer-Transport der App. */
enum class AppMode {
    /** Über einen zentralen MQTT-Broker (z. B. Mosquitto), koexistiert mit Homey. */
    MQTT,

    /** Direkt zu jedem ESPHome-Gerät über dessen Web-Server (REST/SSE), ohne Broker. */
    DIRECT,
    ;

    companion object {
        fun fromName(value: String?): AppMode =
            entries.firstOrNull { it.name == value } ?: MQTT
    }
}
