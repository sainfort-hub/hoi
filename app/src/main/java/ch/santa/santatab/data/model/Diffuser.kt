package ch.santa.santatab.data.model

/**
 * Ein per MQTT-Discovery gefundener HIDE-Diffuser (ESPHome-`fan`-Entität).
 *
 * Die Topics stammen direkt aus der Discovery-Konfiguration, die ESPHome
 * beim Start retained veröffentlicht. Dadurch funktioniert die App
 * unabhängig von der konkreten Geräte-Benennung.
 *
 * Der Drehzahl-Wert ("Intensität") liegt im Bereich [speedRangeMin, speedRangeMax].
 * Bei ESPHome mit `speed_count: 10` sind das die Stufen 1..10 wie im Original.
 */
data class Diffuser(
    val id: String,
    val name: String,
    val commandTopic: String,
    val stateTopic: String?,
    val payloadOn: String = "ON",
    val payloadOff: String = "OFF",
    val percentageCommandTopic: String? = null,
    val percentageStateTopic: String? = null,
    val speedRangeMin: Int = 1,
    val speedRangeMax: Int = 100,
    val availabilityTopic: String? = null,
    val payloadAvailable: String = "online",
    val payloadNotAvailable: String = "offline",
    // --- Live-Zustand (wird über die State-Topics aktualisiert) ---
    val isOn: Boolean = false,
    val level: Int = speedRangeMin,
    val available: Boolean = true,
) {
    /** Anzahl regelbarer Stufen (z. B. 10). */
    val stepCount: Int get() = (speedRangeMax - speedRangeMin + 1).coerceAtLeast(1)

    val supportsSpeed: Boolean get() = percentageCommandTopic != null && stepCount > 1

    /** Aktuelle Stufe als 1-basierter Index für die Anzeige. */
    val displayStep: Int get() = (level - speedRangeMin + 1).coerceIn(1, stepCount)

    fun clampLevel(value: Int): Int = value.coerceIn(speedRangeMin, speedRangeMax)
}
