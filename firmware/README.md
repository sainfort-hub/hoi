# HIDE-Diffuser Firmware (ESPHome)

ESPHome-Konfiguration für den Hardware-Ersatz (Weg B): Die originale Logikplatine
wird durch einen **Seeed Studio XIAO ESP32-C3** ersetzt, der den 5-V-Lüfter über
ein **D4184-MOSFET-Modul** per PWM (20 kHz) regelt.

## Aufbau

| Datei | Zweck |
|-------|-------|
| `common/hide-diffuser.yaml` | Gemeinsames Paket (WLAN, MQTT, Web-Server, Zeit/Taktung, Lüfter, Taster, LED) |
| `duft-wohnzimmer.yaml` | Gerät 1 |
| `duft-schlafzimmer.yaml` | Gerät 2 |
| `duft-eingang.yaml` | Gerät 3 |
| `secrets.yaml.example` | Vorlage für Zugangsdaten |

Die Raumnamen in den Gerätedateien sind Platzhalter – nach Bedarf anpassen.

## Verdrahtung mit MOSFET-Modul

```
Netzteil-Platine 5V ──► XIAO 5V-Pin  ──┬── 470 µF ── GND
                                       │
Netzteil-Platine 5V ──► Modul VIN+ ────┤
Lüfter Rot (+)      ──► Modul VIN+     │
Lüfter Schwarz (−)  ──► Modul OUT−     │
Lüfter Gelb         ──  offen          │
XIAO GPIO4 (D2)     ──► Modul SIG (PWM, 20 kHz)
XIAO GND            ──► Modul GND ─────┴── Netzteil-GND (gemeinsam)
1N5819: Kathode an +5V, Anode an OUT− (Drain)

optional: XIAO GPIO5 (D3) ► Taster SW1 gegen GND · XIAO GPIO6 (D4) ─[330R]─► LED DL2
```

Das Modul bringt Gate-Widerstand, Pulldown und Schraubklemmen mit – die 100 Ω /
10 kΩ aus dem ursprünglichen Konzept entfallen. Details und Messpunkte:
`docs/HARDWARE_STATUS.md`, Abschnitt 4.

> ⚠️ **Sicherheit:** Das Netzteil ist **nicht galvanisch getrennt** (bestätigt).
> Sobald 230 V anliegen: kein USB, kein Programmieradapter, kein PC am Gerät.
> Flashen ausschliesslich **stromlos per USB**, im Betrieb nur OTA. Messen nur mit
> batteriebetriebenem Multimeter.

## Flashen

```bash
# Einmalig: Zugangsdaten anlegen
cp secrets.yaml.example secrets.yaml   # danach ausfüllen

# Erstes Flashen per USB (XIAO stromlos, NICHT am Netzteil!)
esphome run duft-wohnzimmer.yaml

# Weitere Updates laufen anschliessend drahtlos (OTA)
```

Nach dem `min_power`-Feintuning (siehe Kommentar im Paket) die gleiche Firmware
auf Gerät 2 und 3 übertragen.

## Anbindung

- **MQTT-Discovery** (`discovery_prefix: homeassistant`) → App-MQTT-Modus + Homey
- **Web-Server** (REST + SSE, Port 80) → App-Direkt-Modus ohne Broker
- **Autonome Taktung** (SNTP, stündlich, Nachtruhe 22–6 Uhr) mit den persistenten
  Parametern *Automatik*, *Grundstufe*, *Sekunden pro Stunde*

Eine Firmware bedient beide App-Betriebsarten (mit oder ohne Broker).
