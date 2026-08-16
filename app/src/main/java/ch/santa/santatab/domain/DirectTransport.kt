package ch.santa.santatab.domain

import ch.santa.santatab.data.model.ConnectionState
import ch.santa.santatab.data.model.ConnectionStatus
import ch.santa.santatab.data.model.Diffuser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException

/**
 * Steuerung direkt zu jedem ESPHome-Gerät über dessen Web-Server – ohne Broker.
 *
 * - Zustand: Server-Sent-Events unter `http://<host>/events` (Live-Updates)
 * - Steuern: REST, z. B. `POST /fan/<object_id>/turn_on?speed_level=5`
 *
 * Die Geräte-ID in der App ist `"<host>|<object_id>"`, damit mehrere Geräte
 * mit gleich benannten Entitäten eindeutig bleiben.
 */
class DirectTransport(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
) : DiffuserTransport {

    private val devices = MutableStateFlow<Map<String, Diffuser>>(emptyMap())
    private val hostConnected = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override val diffusers: StateFlow<List<Diffuser>> = devices
        .map { map -> map.values.sortedBy { it.name.lowercase() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var hosts: List<String> = emptyList()
    private var supervisor: Job? = null

    fun updateHosts(hosts: List<String>) {
        this.hosts = hosts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun start() {
        stop()
        devices.value = emptyMap()
        hostConnected.value = hosts.associateWith { false }
        recomputeConnection()
        if (hosts.isEmpty()) return

        supervisor = scope.launch {
            hosts.forEach { host -> launch { runHost(host) } }
        }
    }

    override fun stop() {
        supervisor?.cancel()
        supervisor = null
        hostConnected.value = emptyMap()
        _connectionState.value = ConnectionState.Disconnected
    }

    private suspend fun runHost(host: String) {
        while (coroutineContext.isActive) {
            val finished = CompletableDeferred<Unit>()
            try {
                val source = openEvents(host, finished)
                try {
                    finished.await()
                } finally {
                    source.cancel()
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                // Ungültiger Host o. ä. – nach Pause erneut versuchen.
            }
            setHostConnected(host, false)
            delay(RETRY_DELAY_MS)
        }
    }

    private fun openEvents(host: String, finished: CompletableDeferred<Unit>): EventSource {
        val request = Request.Builder().url("http://$host/events").build()
        val factory = EventSources.createFactory(client)
        return factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                setHostConnected(host, true)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type == null || type == "state") parseState(host, data)
            }

            override fun onClosed(eventSource: EventSource) {
                if (!finished.isCompleted) finished.complete(Unit)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!finished.isCompleted) finished.complete(Unit)
            }
        })
    }

    private fun parseState(host: String, data: String) {
        val json = runCatching { JSONObject(data) }.getOrNull() ?: return
        val entityId = json.optString("id")
        if (!entityId.startsWith("fan-")) return
        val objectId = entityId.removePrefix("fan-")
        val id = "$host|$objectId"

        val speedCount = json.optInt("speed_count", 10).coerceAtLeast(1)
        val isOn = when {
            json.has("state") -> json.optString("state").equals("ON", ignoreCase = true)
            else -> json.optInt("value", 0) > 0
        }
        val name = json.optString("name").ifBlank { prettify(objectId) }

        devices.update { current ->
            val existing = current[id]
            val level = if (json.has("speed_level")) {
                json.optInt("speed_level").coerceIn(1, speedCount)
            } else {
                existing?.level ?: 1
            }
            val updated = (existing ?: Diffuser(id = id, name = name)).copy(
                name = name,
                speedRangeMin = 1,
                speedRangeMax = speedCount,
                isOn = isOn,
                level = level,
                available = true,
            )
            current + (id to updated)
        }
    }

    private fun setHostConnected(host: String, connected: Boolean) {
        hostConnected.update { it + (host to connected) }
        if (!connected) {
            // Geräte dieses Hosts als offline markieren.
            devices.update { current ->
                current.mapValues { (_, d) ->
                    if (d.id.substringBefore('|') == host) d.copy(available = false) else d
                }
            }
        }
        recomputeConnection()
    }

    private fun recomputeConnection() {
        val map = hostConnected.value
        _connectionState.value = when {
            map.isEmpty() -> ConnectionState(ConnectionStatus.DISCONNECTED, "Keine Geräte konfiguriert")
            map.values.any { it } -> ConnectionState(
                ConnectionStatus.CONNECTED,
                "${map.values.count { it }}/${map.size} erreichbar",
            )
            else -> ConnectionState(ConnectionStatus.CONNECTING, "Verbinde mit Geräten …")
        }
    }

    override fun setPower(id: String, on: Boolean) {
        val (host, obj) = split(id) ?: return
        post("http://$host/fan/$obj/${if (on) "turn_on" else "turn_off"}")
        optimistic(id) { it.copy(isOn = on) }
    }

    override fun setStep(id: String, step: Int) {
        val (host, obj) = split(id) ?: return
        val device = devices.value[id] ?: return
        val level = device.levelForStep(step)
        post("http://$host/fan/$obj/turn_on?speed_level=$level")
        optimistic(id) { it.copy(level = level, isOn = true) }
    }

    private fun post(url: String) {
        val request = Request.Builder().url(url).post(ByteArray(0).toRequestBody(null)).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = Unit
            override fun onResponse(call: Call, response: Response) = response.close()
        })
    }

    private fun split(id: String): Pair<String, String>? {
        val i = id.indexOf('|')
        if (i <= 0 || i == id.length - 1) return null
        return id.substring(0, i) to id.substring(i + 1)
    }

    private inline fun optimistic(id: String, transform: (Diffuser) -> Diffuser) {
        devices.update { current ->
            val d = current[id] ?: return@update current
            current + (id to transform(d))
        }
    }

    private fun prettify(objectId: String): String =
        objectId.replace('_', ' ').replace('-', ' ')
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private companion object {
        const val RETRY_DELAY_MS = 3000L
    }
}
