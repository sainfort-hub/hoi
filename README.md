# SantaTAB

Android-App zur **lokalen Steuerung** der Revoltab-HIDE-Diffuser – ohne Cloud,
im eigenen WLAN, nach dem Sonos-Prinzip.

Der Hersteller Revoltab ist insolvent und die Afero-Cloud abgeschaltet; die
Geräte werden gemäss dem technischen Konzept (Weg B) auf **ESPHome** umgebaut
und lokal über einen **MQTT-Broker** gesteuert. SantaTAB ist die Bedien-App dazu.

## Was die App kann

- **Ein/Aus** je Diffuser
- **Intensität** 1–10 (Lüfterdrehzahl) per Schieberegler
- **Statusrückmeldung** (läuft / aus / offline, aktuelle Stufe)
- **Szenen** aus dem Betriebskonzept: Grundbetrieb, Ankunft, Gäste, Nacht
- **Automatische Geräteerkennung** über MQTT-Discovery – keine manuelle Geräte-Konfiguration nötig

Läuft parallel zu Homey (beide sprechen denselben Broker).

## Architektur

```
┌────────────┐   MQTT    ┌──────────────┐   MQTT-Discovery   ┌─────────────────┐
│  SantaTAB  │◄─────────►│  Mosquitto   │◄──────────────────►│ HIDE (ESPHome)  │
│  (Android) │           │   Broker     │                    │ ESP32-C3 + Fan  │
└────────────┘           └──────────────┘                    └─────────────────┘
        ▲                        ▲
        │ Ein/Aus, Stufe,        │ (parallel)
        │ Szenen, Status         ▼
                          ┌──────────────┐
                          │    Homey     │  Zeitpläne, Szenen, Sprache
                          └──────────────┘
```

- **Kotlin + Jetpack Compose (Material 3)**, MVVM
- **Eclipse Paho** MQTT-Client
- **DataStore** für die Broker-Einstellungen
- Discovery-Konfiguration wird direkt geparst (Home-Assistant-Format), daher
  robust gegenüber ESPHome-Versionen und Gerätenamen.

Quellcode-Übersicht:

| Bereich | Pfad |
|---------|------|
| MQTT-Client | `app/.../data/mqtt/MqttManager.kt` |
| Discovery-Parser | `app/.../data/mqtt/DiscoveryParser.kt` |
| Steuerlogik | `app/.../domain/DiffuserRepository.kt` |
| UI (Home/Settings) | `app/.../ui/` |
| Gerätefirmware | `firmware/` |

## Einrichten

1. **Broker:** Einen MQTT-Broker (z. B. Mosquitto) im eigenen Netz betreiben.
2. **Geräte:** Firmware aus `firmware/` flashen – siehe `firmware/README.md`.
3. **App:** In SantaTAB unter *Einstellungen* Host/IP, Port (Standard 1883) und
   ggf. Zugangsdaten des Brokers eintragen. Die Diffuser erscheinen automatisch.

## Bauen

```bash
./gradlew assembleDebug      # APK unter app/build/outputs/apk/debug/
```

Voraussetzungen: JDK 17, Android SDK (compileSdk 35). Für die Installation auf
einem Tablet/Handy: `./gradlew installDebug` bei angeschlossenem Gerät.

## Umfang

Drei Geräte im Eigengebrauch. Die eigentliche Zeittaktung der Szenen
(z. B. „3 Min pro Stunde") übernimmt Homey; SantaTAB bietet die Szenen als
direkten manuellen Zugriff und die laufende Bedienung.
