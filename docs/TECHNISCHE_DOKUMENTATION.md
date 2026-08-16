# SantaTAB – Technische Dokumentation

Stand: Version 1.x · Ergänzung zum Reverse-Engineering-Bericht «Revoltab HIDE».

Dieses Dokument beschreibt die **Schnittstellen zwischen App und Gerät** sowie die
**Gerätekonfiguration** nach dem Umbau (Weg B). Es ist so gehalten, dass du es in
deinen Reverse-Engineering-Bericht übernehmen und daraus **gezielte Inputs für
weitere Umprogrammierungen** ableiten kannst. Am Ende steht eine Eingabevorlage.

---

## 1. Systemüberblick

```
                    ┌───────────────────── SantaTAB (Android) ─────────────────────┐
                    │  UI (Compose)  →  DiffuserRepository  →  Transport-Schicht    │
                    └───────────────┬──────────────────────────────┬───────────────┘
                                    │ Modus MQTT                    │ Modus DIREKT
                                    ▼                               ▼
                            ┌──────────────┐               (HTTP REST + SSE, Port 80)
                            │  MQTT-Broker │                        │
                            │  (Mosquitto) │                        │
                            └──────┬───────┘                        │
                MQTT-Discovery /   │                                │
                Command/State      ▼                                ▼
                            ┌───────────────────── HIDE-Diffuser (ESP32-C3 / ESPHome) ─────────┐
                            │  fan (PWM GPIO4)  ·  Automatik-Switch  ·  Zeit (SNTP)  ·  Taster  │
                            └──────────────────────────────────────────────────────────────────┘
```

Es gibt **zwei gleichwertige Steuerwege**, die dieselbe Firmware bedient:

| Modus | Transport | Discovery | Homey parallel | Broker nötig |
|-------|-----------|-----------|----------------|--------------|
| MQTT | MQTT über Broker | automatisch (HA-Format) | ja | ja |
| Direkt | HTTP REST + SSE zum Gerät | manuell (Geräte-IPs) | nein (über diesen Kanal) | nein |

Die **autonome Zeittaktung** läuft unabhängig von beiden auf dem Gerät selbst.

---

## 2. Gerät nach Umbau (ESPHome)

- **Controller:** ESP32-C3 Super Mini (ersetzt die Originalplatine)
- **Aktor:** SUNON UB5U3-500 Lüfter, 5 V / 72 mA, über N-MOSFET an **GPIO4** (PWM, 25 kHz)
- **Optional:** Taster an **GPIO5** (Pullup), Status-LED an **GPIO6** (330 Ω)
- **Firmware-Quelle:** `firmware/common/hide-diffuser.yaml` (+ Gerätedateien)

### Exponierte Entitäten

| Entität | Typ | object_id (Beispiel) | Zweck |
|---------|-----|----------------------|-------|
| `${room}` | `fan` (speed, 10 Stufen) | `duft_wohnzimmer` | Ein/Aus + Intensität 1–10 |
| `${room} Automatik` | `switch` | `duft_wohnzimmer_automatik` | Autonome Taktung ein/aus |
| `${room} Grundstufe` | `number` (1–10) | `duft_wohnzimmer_grundstufe` | Stufe der Taktung |
| `${room} Sekunden pro Stunde` | `number` (0–3600) | `duft_wohnzimmer_sekunden_pro_stunde` | Dauer pro Stunde |
| `${room} Taster` | `binary_sensor` | – | Physischer Taster |
| `${room} Status-LED` | `light` | – | LED |

> Die `object_id` entsteht aus dem Entitätsnamen (Kleinschreibung, Leerzeichen → `_`).

---

## 3. Schnittstelle A — MQTT

Aktiviert durch `mqtt:` in der Firmware. `discovery: true`, `discovery_prefix: homeassistant`.

### Discovery (retained)
```
homeassistant/fan/<node>/<object_id>/config      → JSON (command/state/percentage-Topics, speed_range …)
```
Die App abonniert `homeassistant/fan/#`, liest die Topics aus der Config und
folgt ihnen. Relevante JSON-Felder (voll oder abgekürzt): `command_topic`/`cmd_t`,
`state_topic`/`stat_t`, `payload_on`/`pl_on`, `payload_off`/`pl_off`,
`percentage_command_topic`/`pct_cmd_t`, `percentage_state_topic`/`pct_stat_t`,
`speed_range_min`/`spd_rng_min`, `speed_range_max`/`spd_rng_max`,
`availability_topic`/`avty_t`. `~` als Basistopic wird aufgelöst.

### Steuern / Beobachten (Topics stammen aus der Discovery, typisch)
```
<node>/fan/<object_id>/command                 ON | OFF          (schreiben)
<node>/fan/<object_id>/state                   ON | OFF          (lesen)
<node>/fan/<object_id>/speed_level/command     1..10             (schreiben)
<node>/fan/<object_id>/speed_level/state       1..10             (lesen)
<node>/status                                  online | offline  (Verfügbarkeit)
```
Die App verlässt sich **nicht** auf feste Topic-Namen, sondern nimmt die aus der
Discovery-Config. Der Drehzahlwert wird als ganze Zahl im `speed_range` übertragen.

Code: `data/mqtt/DiscoveryParser.kt`, `domain/MqttTransport.kt`.

---

## 4. Schnittstelle B — Direkt (REST + SSE)

Aktiviert durch `web_server:` (Port 80, `version: 2`, `local: true`). Basis-URL
`http://<host>/` (host = IP oder `<device_name>.local`).

### Befehle (REST)
```
POST http://<host>/fan/<object_id>/turn_on                     → einschalten
POST http://<host>/fan/<object_id>/turn_on?speed_level=<1..10> → einschalten mit Stufe
POST http://<host>/fan/<object_id>/turn_off                    → ausschalten
POST http://<host>/switch/<object_id>/turn_on|turn_off         → z. B. Automatik
POST http://<host>/number/<object_id>/set?value=<n>            → z. B. Grundstufe / Sekunden
```

### Zustand (Server-Sent Events)
```
GET http://<host>/events        → Stream von "state"-Events, je Entität ein JSON
```
Beispiel-Event-Daten (Fan):
```json
{ "id": "fan-duft_wohnzimmer", "name": "Duft Wohnzimmer",
  "state": "ON", "value": 40, "speed_level": 4, "speed_count": 10 }
```
Die App filtert `id`-Präfix `fan-`, leitet `object_id` ab, liest `state`,
`speed_level`, `speed_count`. Fällt die SSE-Verbindung aus, gilt das Gerät als offline.

Code: `domain/DirectTransport.kt` (Geräte-ID in der App = `"<host>|<object_id>"`).

> ⚠️ **Auf echter Hardware zu verifizieren:** exakter Query-Parametername
> (`speed_level`) und die genauen Feldnamen im SSE-JSON können je nach
> ESPHome-Version leicht abweichen. Beim ersten Test mit einem realen Gerät
> gegenprüfen und bei Bedarf hier korrigieren.

---

## 5. Autonome Zeittaktung (auf dem Gerät)

Läuft ohne App/Broker, gesteuert über die Entitäten aus Abschnitt 2:

- **Auslöser:** zu jeder vollen Stunde (`cron: "0 0 * * * *"`).
- **Bedingung:** `Automatik` = an · Zeit gültig (SNTP) · `Sekunden pro Stunde` > 0 · Stunde 6–21 (Nachtruhe 22:00–06:00).
- **Wirkung:** Lüfter auf `Grundstufe` ein, `Sekunden pro Stunde` lang, dann aus.

Parameter sind persistent (`restore_value`) und per REST/MQTT/Homey/App änderbar.
Grundlogik (Nachtfenster, Stundenraster) ist in der YAML fest; Änderung = OTA-Update.

Code: `firmware/common/hide-diffuser.yaml` (Abschnitt `time:`).

---

## 6. App-Architektur (Kurzreferenz)

| Schicht | Datei(en) |
|---------|-----------|
| Einstellungen/Persistenz | `data/settings/SettingsRepository.kt`, `data/model/AppSettings.kt`, `AppMode.kt`, `BrokerSettings.kt` |
| Modell | `data/model/Diffuser.kt`, `Scene.kt`, `ConnectionState.kt` |
| MQTT | `data/mqtt/MqttManager.kt`, `DiscoveryParser.kt`, `MqttFan.kt` |
| Transport | `domain/DiffuserTransport.kt`, `MqttTransport.kt`, `DirectTransport.kt` |
| Orchestrierung | `domain/DiffuserRepository.kt` (schaltet je Modus), `AppContainer.kt` |
| UI | `ui/home/`, `ui/settings/`, `ui/guide/`, `ui/components/` |

Transport wird zur Laufzeit per Modus umgeschaltet; die Steuerbefehle
(`setPower`, `setStep`, `applyScene`) sind für beide Wege identisch.

---

## 7. So gibst du mir neue Inputs (Eingabevorlage)

Wenn du am Gerät etwas änderst oder neu umprogrammierst, gib mir bitte die
folgenden Angaben – dann passe ich App und/oder Firmware zielgerichtet an:

```
### Änderung / Ziel
(Was soll neu können? z. B. „zweiter Lüfter“, „Duftpause per App“, „Kapsel-LED“)

### Gerät / Firmware
- Betroffene(s) Gerät(e):            (z. B. duft-wohnzimmer)
- Neue/geänderte GPIOs:              (Pin → Funktion)
- Neue/geänderte Entitäten:          (Typ, Name, Wertebereich)
- Geänderte REST-/MQTT-Endpunkte:    (falls bekannt)

### Beobachtungen am echten Gerät (wichtig!)
- Antwort auf  GET http://<host>/events  (1 Fan-Event-JSON einfügen):
- Funktioniert POST .../turn_on?speed_level=N ?   ja / nein / anders:
- ESPHome-Version:                    (esphome/x.y.z)

### App-Verhalten
(Was soll die App anders/zusätzlich tun?)
```

Je mehr davon ausgefüllt ist, desto direkter kann ich umsetzen. Für Firmware-Fragen
reicht oft schon ein `GET /events`-Mitschnitt und die Liste der gewünschten Entitäten.

---

## 8. Offene Annahmen (auf realer HW zu bestätigen)

1. `min_power: 0.25` – Anlaufschwelle des Lüfters empirisch feinjustieren.
2. REST-Parametername `speed_level` und SSE-JSON-Felder (Abschnitt 4) gegen die
   tatsächliche ESPHome-Version prüfen.
3. SNTP braucht eine Zeitquelle (Internet oder lokaler NTP-Server) für die Taktung.
4. Anzeigenamen im Direkt-Modus stammen aus dem SSE-`name` bzw. werden aus der
   `object_id` abgeleitet, wenn kein Name geliefert wird.
