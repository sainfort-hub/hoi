package ch.santa.santatab.data.mqtt

/**
 * MQTT-spezifische Beschreibung einer Fan-Entität, gewonnen aus der
 * Discovery-Konfiguration. Enthält alle Topics zum Steuern und Beobachten.
 */
data class MqttFan(
    val id: String,
    val name: String,
    val commandTopic: String,
    val stateTopic: String?,
    val payloadOn: String,
    val payloadOff: String,
    val percentageCommandTopic: String?,
    val percentageStateTopic: String?,
    val speedRangeMin: Int,
    val speedRangeMax: Int,
    val availabilityTopic: String?,
    val payloadAvailable: String,
    val payloadNotAvailable: String,
)
