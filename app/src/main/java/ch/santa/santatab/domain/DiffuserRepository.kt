package ch.santa.santatab.domain

import ch.santa.santatab.data.model.BrokerSettings
import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.Diffuser
import ch.santa.santatab.data.model.Scene
import ch.santa.santatab.data.mqtt.DiscoveryParser
import ch.santa.santatab.data.mqtt.MqttManager
import ch.santa.santatab.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Zentrale Steuerlogik: hält die entdeckten Diffuser, hört auf ihre
 * Zustands-Topics und schickt Befehle über den [MqttManager].
 */
class DiffuserRepository(
    private val mqtt: MqttManager,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val devices = MutableStateFlow<Map<String, Diffuser>>(emptyMap())

    /** Alle bekannten Diffuser, alphabetisch nach Name sortiert. */
    val diffusers: StateFlow<List<Diffuser>> = devices
        .map { map -> map.values.sortedBy { it.name.lowercase() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val connectionState: StateFlow<ConnectionState> = mqtt.connectionState

    val scenes: List<Scene> = Scene.DEFAULTS

    private var currentSettings: BrokerSettings = BrokerSettings()

    fun start() {
        mqtt.onConnected = { onBrokerConnected() }

        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                currentSettings = settings
                devices.value = emptyMap()
                mqtt.connect(settings)
            }
        }

        scope.launch {
            mqtt.events.collect { event -> handleEvent(event.topic, event.payload) }
        }
    }

    private fun onBrokerConnected() {
        // Discovery-Topics für alle Fan-Entitäten abonnieren.
        mqtt.subscribe("${currentSettings.discoveryPrefix}/fan/#")
        // Zustands-Topics bereits bekannter Geräte erneut abonnieren.
        devices.value.values.forEach(::subscribeDeviceTopics)
    }

    private fun handleEvent(topic: String, payload: String) {
        if (DiscoveryParser.isFanConfigTopic(topic)) {
            handleDiscovery(topic, payload)
            return
        }
        applyStateUpdate(topic, payload)
    }

    private fun handleDiscovery(topic: String, payload: String) {
        if (payload.isBlank()) {
            // Leerer Payload = Discovery gelöscht -> Gerät entfernen.
            val objectId = DiscoveryParser.objectIdFromTopic(topic)
            devices.update { current ->
                current.filterValues { it.id != objectId && it.name != objectId }
            }
            return
        }
        val parsed = DiscoveryParser.parse(topic, payload) ?: return
        devices.update { current ->
            val existing = current[parsed.id]
            // Live-Zustand über die neue Definition retten.
            val merged = if (existing != null) {
                parsed.copy(isOn = existing.isOn, level = existing.level, available = existing.available)
            } else {
                parsed
            }
            current + (parsed.id to merged)
        }
        subscribeDeviceTopics(parsed)
    }

    private fun subscribeDeviceTopics(device: Diffuser) {
        device.stateTopic?.let { mqtt.subscribe(it) }
        device.percentageStateTopic?.let { mqtt.subscribe(it) }
        device.availabilityTopic?.let { mqtt.subscribe(it) }
    }

    private fun applyStateUpdate(topic: String, payload: String) {
        devices.update { current ->
            var changed = false
            val updated = current.mapValues { (_, device) ->
                when (topic) {
                    device.stateTopic -> {
                        changed = true
                        device.copy(isOn = payload.equals(device.payloadOn, ignoreCase = true))
                    }
                    device.percentageStateTopic -> {
                        val value = payload.trim().toIntOrNull()
                        if (value != null) {
                            changed = true
                            device.copy(level = device.clampLevel(value))
                        } else {
                            device
                        }
                    }
                    device.availabilityTopic -> {
                        changed = true
                        device.copy(available = payload.equals(device.payloadAvailable, ignoreCase = true))
                    }
                    else -> device
                }
            }
            if (changed) updated else current
        }
    }

    // --- Steuerbefehle -------------------------------------------------------

    fun setPower(id: String, on: Boolean) {
        val device = devices.value[id] ?: return
        mqtt.publish(device.commandTopic, if (on) device.payloadOn else device.payloadOff, qos = 1)
        optimistic(id) { it.copy(isOn = on) }
    }

    /** Setzt die Intensität (1-basierte Stufe) und schaltet das Gerät ein. */
    fun setStep(id: String, step: Int) {
        val device = devices.value[id] ?: return
        val target = device.speedRangeMin + (step - 1)
        val level = device.clampLevel(target)
        device.percentageCommandTopic?.let { mqtt.publish(it, level.toString(), qos = 1) }
        if (!device.isOn) {
            mqtt.publish(device.commandTopic, device.payloadOn, qos = 1)
        }
        optimistic(id) { it.copy(level = level, isOn = true) }
    }

    fun applyScene(scene: Scene) {
        devices.value.values.forEach { device ->
            if (scene.turnsOff) {
                setPower(device.id, false)
            } else {
                setStep(device.id, scene.targetStep.coerceAtMost(device.stepCount))
            }
        }
    }

    private inline fun optimistic(id: String, transform: (Diffuser) -> Diffuser) {
        devices.update { current ->
            val device = current[id] ?: return@update current
            current + (id to transform(device))
        }
    }
}
