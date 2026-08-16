package ch.santa.santatab.domain

import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.Diffuser
import kotlinx.coroutines.flow.StateFlow

/**
 * Gemeinsame Schnittstelle für die beiden Steuerwege (MQTT-Broker und Direkt).
 * Der [DiffuserRepository] schaltet je nach gewähltem Modus auf den passenden.
 */
interface DiffuserTransport {
    val diffusers: StateFlow<List<Diffuser>>
    val connectionState: StateFlow<ConnectionState>

    /** Verbindung aufbauen / neu aufbauen. */
    fun start()

    /** Verbindung schliessen und Zustand zurücksetzen. */
    fun stop()

    fun setPower(id: String, on: Boolean)

    /** Setzt die 1-basierte Intensitätsstufe und schaltet ein. */
    fun setStep(id: String, step: Int)
}
