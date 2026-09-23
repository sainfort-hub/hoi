package ch.santa.santatab.domain

import ch.santa.santatab.data.model.BrokerSettings
import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.Diffuser
import ch.santa.santatab.data.mqtt.DiscoveryParser
import ch.santa.santatab.data.mqtt.MqttFan
import ch.santa.santatab.data.mqtt.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Steuerung über einen MQTT-Broker mit automatischer Discovery. */
class MqttTransport(
    private val mqtt: MqttManager,
    private val scope: CoroutineScope,
) : DiffuserTransport {

    private data class Entry(val fan: MqttFan, val state: Diffuser)

    private val entries = MutableStateFlow<Map<String, Entry>>(emptyMap())

    override val diffusers: StateFlow<List<Diffuser>> = entries
        .map { map -> map.values.map { it.state }.sortedBy { it.name.lowercase() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val connectionState: StateFlow<ConnectionState> = mqtt.connectionState

    private var broker: BrokerSettings = BrokerSettings()

    init {
        mqtt.onConnected = { onConnected() }
        scope.launch {
            mqtt.events.collect { handleEvent(it.topic, it.payload) }
        }
    }

    fun updateBroker(settings: BrokerSettings) {
        broker = settings
    }

    override fun start() {
        entries.value = emptyMap()
        mqtt.connect(broker)
    }

    override fun stop() {
        mqtt.disconnect()
        entries.value = emptyMap()
    }

    private fun onConnected() {
        mqtt.subscribe("${broker.discoveryPrefix}/fan/#")
        entries.value.values.forEach { subscribeTopics(it.fan) }
    }

    private fun handleEvent(topic: String, payload: String) {
        if (DiscoveryParser.isFanConfigTopic(topic)) {
            handleDiscovery(topic, payload)
        } else {
            applyStateUpdate(topic, payload)
        }
    }

    private fun handleDiscovery(topic: String, payload: String) {
        if (payload.isBlank()) {
            val objectId = DiscoveryParser.objectIdFromTopic(topic)
            entries.update { current -> current.filterValues { it.fan.id != objectId } }
            return
        }
        val fan = DiscoveryParser.parse(topic, payload) ?: return
        entries.update { current ->
            val existing = current[fan.id]?.state
            val state = (existing ?: Diffuser(id = fan.id, name = fan.name)).copy(
                name = fan.name,
                speedRangeMin = fan.speedRangeMin,
                speedRangeMax = fan.speedRangeMax,
            )
            current + (fan.id to Entry(fan, state))
        }
        subscribeTopics(fan)
    }

    private fun subscribeTopics(fan: MqttFan) {
        fan.stateTopic?.let { mqtt.subscribe(it) }
        fan.percentageStateTopic?.let { mqtt.subscribe(it) }
        fan.availabilityTopic?.let { mqtt.subscribe(it) }
    }

    private fun applyStateUpdate(topic: String, payload: String) {
        entries.update { current ->
            var changed = false
            val updated = current.mapValues { (_, entry) ->
                val fan = entry.fan
                val state = when (topic) {
                    fan.stateTopic -> {
                        changed = true
                        entry.state.copy(isOn = payload.equals(fan.payloadOn, ignoreCase = true))
                    }
                    fan.percentageStateTopic -> {
                        val value = payload.trim().toIntOrNull()
                        if (value != null) {
                            changed = true
                            entry.state.copy(level = entry.state.clampLevel(value))
                        } else {
                            entry.state
                        }
                    }
                    fan.availabilityTopic -> {
                        changed = true
                        entry.state.copy(available = payload.equals(fan.payloadAvailable, ignoreCase = true))
                    }
                    else -> entry.state
                }
                entry.copy(state = state)
            }
            if (changed) updated else current
        }
    }

    override fun setPower(id: String, on: Boolean) {
        val entry = entries.value[id] ?: return
        mqtt.publish(entry.fan.commandTopic, if (on) entry.fan.payloadOn else entry.fan.payloadOff, qos = 1)
        optimistic(id) { it.copy(isOn = on) }
    }

    override fun setStep(id: String, step: Int) {
        val entry = entries.value[id] ?: return
        val level = entry.state.levelForStep(step)
        entry.fan.percentageCommandTopic?.let { mqtt.publish(it, level.toString(), qos = 1) }
        if (!entry.state.isOn) mqtt.publish(entry.fan.commandTopic, entry.fan.payloadOn, qos = 1)
        optimistic(id) { it.copy(level = level, isOn = true) }
    }

    private inline fun optimistic(id: String, transform: (Diffuser) -> Diffuser) {
        entries.update { current ->
            val entry = current[id] ?: return@update current
            current + (id to entry.copy(state = transform(entry.state)))
        }
    }
}
