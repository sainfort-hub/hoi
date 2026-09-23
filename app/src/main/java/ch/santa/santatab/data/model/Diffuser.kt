package ch.santa.santatab.data.model

/**
 * Ein Diffuser, wie ihn die UI darstellt – transport-unabhängig.
 *
 * Die Drehzahl ("Intensität") liegt im Bereich [speedRangeMin, speedRangeMax].
 * Bei ESPHome mit `speed_count: 10` sind das die Stufen 1..10 wie im Original.
 * Die konkreten MQTT-Topics bzw. REST-Adressen hält der jeweilige Transport.
 */
data class Diffuser(
    val id: String,
    val name: String,
    val speedRangeMin: Int = 1,
    val speedRangeMax: Int = 100,
    val isOn: Boolean = false,
    val level: Int = speedRangeMin,
    val available: Boolean = true,
) {
    /** Anzahl regelbarer Stufen (z. B. 10). */
    val stepCount: Int get() = (speedRangeMax - speedRangeMin + 1).coerceAtLeast(1)

    val supportsSpeed: Boolean get() = stepCount > 1

    /** Aktuelle Stufe als 1-basierter Index für die Anzeige. */
    val displayStep: Int get() = (level - speedRangeMin + 1).coerceIn(1, stepCount)

    fun clampLevel(value: Int): Int = value.coerceIn(speedRangeMin, speedRangeMax)

    /** Rechnet eine 1-basierte Stufe in den Rohwert des Geräts um. */
    fun levelForStep(step: Int): Int = clampLevel(speedRangeMin + (step - 1))
}
