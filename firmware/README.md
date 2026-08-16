# HIDE-Diffuser Firmware (ESPHome)

ESPHome-Konfiguration für den Hardware-Ersatz (Weg B) aus dem technischen
Konzept: Die originale Logikplatine wird durch einen **ESP32-C3 Super Mini**
ersetzt, der den 5-V-Lüfter über eine MOSFET-Treiberstufe per PWM regelt.

## Aufbau

| Datei | Zweck |
|-------|-------|
| `common/hide-diffuser.yaml` | Gemeinsames Paket (WLAN, MQTT, Lüfter, Taster, LED) |
| `duft-wohnzimmer.yaml` | Gerät 1 |
| `duft-schlafzimmer.yaml` | Gerät 2 |
| `duft-eingang.yaml` | Gerät 3 |
| `secrets.yaml.example` | Vorlage für Zugangsdaten |

Die Raumnamen in den Gerätedateien sind Platzhalter – nach Bedarf anpassen.

## Verdrahtung (Konzept, Abschnitt 4.3)

```
Netzteil-Platine 5V ──► ESP32-C3 5V-Eingang
Lüfter rot (+)      ──► +5V
Lüfter schwarz (−)  ──► MOSFET Drain
GPIO4 ─[100R]─► MOSFET Gate ─[10k]─► GND     (PWM)
MOSFET Source       ──► GND (gemeinsam)
Freilaufdiode: Kathode an +5V, Anode an Drain
optional: GPIO5 ► Taster gegen GND · GPIO6 ─[330R]─► Status-LED
```

> **Sicherheit:** Arbeiten nur am ausgebauten, spannungsfreien Gerät. Niemals
> gleichzeitig 230 V (Netzteil) und USB anschliessen – das Netzteil ist evtl.
> nicht galvanisch getrennt.

## Flashen

```bash
# Einmalig: Zugangsdaten anlegen
cp secrets.yaml.example secrets.yaml   # danach ausfüllen

# Erstes Flashen per USB
esphome run duft-wohnzimmer.yaml

# Weitere Updates laufen anschliessend drahtlos (OTA)
```

Nach dem `min_power`-Feintuning (siehe Kommentar im Paket) die gleiche Firmware
auf Gerät 2 und 3 übertragen.

## Anbindung

Die Geräte melden sich per **MQTT-Discovery** (`discovery_prefix: homeassistant`)
am Broker an. Damit erscheinen sie automatisch in **SantaTAB** und lassen sich
parallel in **Homey** (über die MQTT-Client-App) für Zeitpläne und Szenen nutzen.
