package ch.santa.santatab.data.mqtt

import ch.santa.santatab.data.model.Diffuser
import org.json.JSONObject

/**
 * Wandelt eine MQTT-Discovery-Konfiguration (Home-Assistant-Format, wie ESPHome
 * sie veröffentlicht) in ein [Diffuser]-Objekt um.
 *
 * Unterstützt sowohl die ausgeschriebenen Schlüssel (`command_topic`) als auch
 * die abgekürzten (`cmd_t`) sowie die `~`-Basistopic-Ersetzung. Dadurch bleibt
 * die App robust gegenüber ESPHome-Versionsunterschieden.
 */
object DiscoveryParser {

    /** Prüft, ob ein Topic eine Fan-Discovery-Konfiguration ist. */
    fun isFanConfigTopic(topic: String): Boolean =
        topic.contains("/fan/") && topic.endsWith("/config")

    /**
     * Liefert die stabile Geräte-ID zu einem Discovery-Topic, damit ein
     * leerer Payload (Löschung der Discovery) dem richtigen Gerät zugeordnet
     * werden kann.
     */
    fun objectIdFromTopic(topic: String): String =
        topic.removeSuffix("/config").substringAfter("/fan/").replace('/', '_')

    /** Parst den Discovery-Payload. Gibt null zurück, wenn Pflichtfelder fehlen. */
    fun parse(topic: String, payload: String): Diffuser? {
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        val base = json.optString("~", "")

        fun topicOf(vararg keys: String): String? {
            val raw = firstString(json, *keys) ?: return null
            return applyBase(raw, base)
        }

        val commandTopic = topicOf("command_topic", "cmd_t") ?: return null

        val availability = json.optJSONArray("availability")
        val availabilityEntry = availability?.optJSONObject(0)

        val availabilityTopic = topicOf("availability_topic", "avty_t")
            ?: availabilityEntry?.let { applyBase(firstString(it, "topic", "t") ?: return@let null, base) }

        val payloadAvailable = firstString(json, "payload_available", "pl_avail")
            ?: availabilityEntry?.let { firstString(it, "payload_available", "pl_avail") }
            ?: "online"
        val payloadNotAvailable = firstString(json, "payload_not_available", "pl_not_avail")
            ?: availabilityEntry?.let { firstString(it, "payload_not_available", "pl_not_avail") }
            ?: "offline"

        val id = firstString(json, "unique_id", "uniq_id") ?: objectIdFromTopic(topic)
        val name = firstString(json, "name") ?: id

        return Diffuser(
            id = id,
            name = name,
            commandTopic = commandTopic,
            stateTopic = topicOf("state_topic", "stat_t"),
            payloadOn = firstString(json, "payload_on", "pl_on") ?: "ON",
            payloadOff = firstString(json, "payload_off", "pl_off") ?: "OFF",
            percentageCommandTopic = topicOf("percentage_command_topic", "pct_cmd_t"),
            percentageStateTopic = topicOf("percentage_state_topic", "pct_stat_t"),
            speedRangeMin = firstInt(json, "speed_range_min", "spd_rng_min") ?: 1,
            speedRangeMax = firstInt(json, "speed_range_max", "spd_rng_max") ?: 100,
            availabilityTopic = availabilityTopic,
            payloadAvailable = payloadAvailable,
            payloadNotAvailable = payloadNotAvailable,
        )
    }

    private fun applyBase(value: String, base: String): String {
        if (base.isEmpty()) return value
        return when {
            value.startsWith("~") -> base + value.substring(1)
            value.endsWith("~") -> value.dropLast(1) + base
            else -> value
        }
    }

    private fun firstString(json: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (json.has(key)) {
                val v = json.optString(key, "")
                if (v.isNotEmpty()) return v
            }
        }
        return null
    }

    private fun firstInt(json: JSONObject, vararg keys: String): Int? {
        for (key in keys) {
            if (json.has(key)) return json.optInt(key)
        }
        return null
    }
}
