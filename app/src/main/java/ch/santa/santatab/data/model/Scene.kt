package ch.santa.santatab.data.model

/**
 * Eine Szene aus dem Betriebskonzept (Abschnitt 6 des technischen Konzepts).
 *
 * Eine Szene setzt alle Diffuser gleichzeitig auf eine Zielstufe.
 * [targetStep] ist 1-basiert (1..stepCount); der Wert 0 bedeutet "aus".
 * Die eigentliche Zeittaktung (z. B. "3 Min pro Stunde") übernimmt Homey —
 * SantaTAB bietet die Szenen als direkten manuellen Zugriff.
 */
data class Scene(
    val id: String,
    val label: String,
    val description: String,
    val targetStep: Int,
) {
    val turnsOff: Boolean get() = targetStep <= 0

    companion object {
        val DEFAULTS: List<Scene> = listOf(
            Scene("grundbetrieb", "Grundbetrieb", "Dezente Grundnote · Stufe 4", targetStep = 4),
            Scene("ankunft", "Ankunft", "Frischer Eindruck · Stufe 8", targetStep = 8),
            Scene("gaeste", "Gäste", "Kurzfristig intensiv · Stufe 10", targetStep = 10),
            Scene("nacht", "Nacht", "Alles aus · Ruhe", targetStep = 0),
        )
    }
}
