package ch.santa.santatab.domain

import ch.santa.santatab.data.model.AppMode
import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.Diffuser
import ch.santa.santatab.data.model.Scene
import ch.santa.santatab.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Fasst die beiden Transportwege zusammen und schaltet je nach gewähltem Modus
 * (MQTT-Broker oder Direkt) auf den passenden. Die Szenen-Logik ist gemeinsam.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiffuserRepository(
    private val mqtt: MqttTransport,
    private val direct: DirectTransport,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _mode = MutableStateFlow(AppMode.MQTT)
    val mode: StateFlow<AppMode> = _mode

    val diffusers: StateFlow<List<Diffuser>> = _mode
        .flatMapLatest { active(it).diffusers }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val connectionState: StateFlow<ConnectionState> = _mode
        .flatMapLatest { active(it).connectionState }
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val scenes: List<Scene> = Scene.DEFAULTS

    fun start() {
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                mqtt.updateBroker(settings.broker)
                direct.updateHosts(settings.directHosts)
                when (settings.mode) {
                    AppMode.MQTT -> { direct.stop(); mqtt.start() }
                    AppMode.DIRECT -> { mqtt.stop(); direct.start() }
                }
                _mode.value = settings.mode
            }
        }
    }

    private fun active(mode: AppMode): DiffuserTransport =
        if (mode == AppMode.DIRECT) direct else mqtt

    fun setPower(id: String, on: Boolean) = active(_mode.value).setPower(id, on)

    fun setStep(id: String, step: Int) = active(_mode.value).setStep(id, step)

    fun applyScene(scene: Scene) {
        val transport = active(_mode.value)
        transport.diffusers.value.forEach { device ->
            if (scene.turnsOff) {
                transport.setPower(device.id, false)
            } else {
                transport.setStep(device.id, scene.targetStep.coerceAtMost(device.stepCount))
            }
        }
    }
}
