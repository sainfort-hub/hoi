# Hardware-Status & Handoff — Revoltab HIDE / SantaTAB

Lebendes Statusdokument für die **Hardware- und Reverse-Engineering-Seite**.
Ergänzt den RE-Bericht «Revoltab HIDE — Projekt-Zämmefassig» und das
«Technische Konzept». Software-/App-Details: siehe
[`TECHNISCHE_DOKUMENTATION.md`](TECHNISCHE_DOKUMENTATION.md).

Stand: laufende Entwicklung · Zielarchitektur **Weg B** (Hardware-Ersatz).

---

## 1. Kurzstatus (Ampel)

| Bereich | Stand | Bemerkung |
|---------|:-----:|-----------|
| Analyse / RE des Originals | 🟢 abgeschlossen | Cloud tot, Flash-CA blockiert, UART gesperrt |
| Zielentscheid | 🟢 Weg B | Logikplatine ersetzen (ESP32-C3 + ESPHome) |
| App SantaTAB | 🟢 gebaut & baubar | MQTT- **und** Direkt-Modus, CI grün |
| ESPHome-Firmware | 🟡 fertig, ungetestet | kompiliert noch nicht auf echter HW verifiziert |
| Hardware-Umbau | 🔴 offen | Löten/Einbau pro Gerät noch nicht erfolgt |
| Verifikation am Gerät | 🔴 offen | REST/SSE, PWM-min_power, SNTP-Taktung prüfen |

Legende: 🟢 erledigt · 🟡 bereit, aber zu verifizieren · 🔴 offen.

---

## 2. Was aus dem Reverse Engineering feststeht

- **Cloud tot:** Afero-Tenant `o80y8sax` DNS gelöscht → kein Original-Betrieb.
- **Netzwerk-Weg blockiert:** DNS-Redirect + TLS gehen, aber Handshake scheitert
  an `UNKNOWN_CA` (Gerät prüft gegen Flash-CA / Afero-Signatur).
- **UART-Flash gesperrt:** eFuse `UART_DOWNLOAD_DIS` gesetzt → kein Reflash über UART.
- **Offen:** JTAG / SPI-Flash-Zugriff (Machbarkeit hängt an Flash-Encryption),
  messbar mit dem bestellten **ESP-PROG-2**.
- **Gerät ist simpel:** Der «Diffusor» ist nur ein 5-V-Lüfter (SUNON UB5U3-500,
  72 mA). Keine Heizung/Piezo. «Intensität» = Lüfterdrehzahl.

➡️ **Konsequenz:** Weg B (eigener ESP32-C3 statt Originalplatine) ist unabhängig
vom Ausgang des Flash-Tests und entfernt zugleich die NFC-Kapselsperre.

---

## 3. Umbau pro Gerät (Weg B) — Checkliste

Pro Diffuser (3 Geräte im Eigengebrauch):

- [ ] Gerät spannungsfrei schalten (230 V!) und öffnen
- [ ] Logikplatine am Flachbandkabel abtrennen (Netzteilhälfte + 5 V bleibt)
- [ ] Treiberstufe aufbauen: N-MOSFET (AO3400A/IRLML2502), 100 Ω Gate, 10 kΩ Pulldown, Freilaufdiode (1N4148/SS14)
- [ ] ESP32-C3 Super Mini einsetzen, verdrahten (siehe unten)
- [ ] ESPHome per **USB** flashen (erstes Mal), danach OTA
- [ ] `min_power` empirisch justieren (Lüfter läuft gerade noch zuverlässig an)
- [ ] Einbau, Isolation zur 230-V-Seite prüfen, Dauerlauf beobachten

### Verdrahtung (Konzept 4.3)

```
Netzteil-Platine 5V ──► ESP32-C3 5V-Eingang
Lüfter rot (+)      ──► +5V
Lüfter schwarz (−)  ──► MOSFET Drain
GPIO4 ─[100R]─► MOSFET Gate ─[10k]─► GND          (PWM, 25 kHz)
MOSFET Source       ──► GND (gemeinsam mit Netzteil-GND)
Freilaufdiode: Kathode an +5V, Anode an Drain
optional: GPIO5 ► Taster gegen GND · GPIO6 ─[330R]─► Status-LED
```

> ⚠️ **Sicherheit:** Arbeiten nur am ausgebauten, spannungsfreien Gerät.
> Niemals gleichzeitig 230 V (Netzteil) und USB — das Netzteil ist evtl. nicht
> galvanisch getrennt. Feste Elektroinstallation in der CH ist bewilligungspflichtig.

---

## 4. Firmware-Stand (ESPHome)

Quelle: `firmware/` (gemeinsames Paket `common/hide-diffuser.yaml` + 3 Gerätedateien).

Bereits enthalten:
- `fan` (speed, 10 Stufen) an GPIO4 (LEDC-PWM 25 kHz, `min_power: 0.25`)
- **MQTT** mit Discovery (für App-MQTT-Modus + Homey), `reboot_timeout: 0s`
- **web_server** (REST + SSE, Port 80, `version: 2`, `local: true`) — für App-Direkt-Modus
- **SNTP-Zeit** + autonome, stündliche Taktung (Nachtruhe 22–6 Uhr)
- Persistente Parameter: `Automatik` (switch), `Grundstufe` (1–10), `Sekunden pro Stunde` (0–3600)
- optional Taster (GPIO5) und Status-LED (GPIO6)

**Eine Firmware bedient beide App-Modi** (mit oder ohne Broker).

---

## 5. Am echten Gerät zu verifizieren (wichtig)

Diese Punkte konnten mangels Hardware noch **nicht** geprüft werden:

1. **PWM-Anlaufschwelle:** `min_power` so weit senken, bis der Lüfter aus dem
   Stand zuverlässig anläuft (Startwert 0.25).
2. **REST-Parametername:** ob `POST /fan/<obj>/turn_on?speed_level=<n>` die Stufe
   korrekt setzt (je nach ESPHome-Version evtl. anderer Parameter).
3. **SSE-JSON-Felder:** `GET /events` → Felder `id`, `state`, `speed_level`,
   `speed_count`, `name` gegenprüfen (siehe Eingabevorlage in der techn. Doku).
4. **MQTT-Discovery:** ob die Fan-Entität `percentage_*`-Topics mit
   `speed_range 1..10` liefert (die App liest die Topics aus der Discovery).
5. **SNTP:** braucht eine Zeitquelle (Internet oder lokaler NTP) — sonst läuft
   die autonome Taktung nicht.

➡️ Sobald ein Gerät läuft: `GET /events`-Mitschnitt + Beobachtungen gemäss
**Eingabevorlage** (Abschnitt 7 der technischen Doku) liefern — dann werden
App/Firmware bei Bedarf angepasst.

---

## 6. Optionaler Weg A (falls Flash-Test positiv)

Nur relevant, wenn der ESP-PROG-2-Flash-Test zeigt, dass der Flash **unverschlüsselt**
ist (lesbarer Text im Dump):

- [ ] Flash-Dump ziehen (JTAG: TP4=TMS, TP6=TCK, TP3=TDI, TP8=TDO, +GND)
- [ ] Dump prüfen: lesbar → Weg A möglich; Rauschen → Flash-Encryption aktiv, nur Weg B
- [ ] Bei lesbar: Client-Cert + Key extrahieren oder Flash-CA ersetzen (Teddycloud-Prinzip)

Weg A erhält die Original-Firmware, **behält aber die NFC-Kapselsperre**.

---

## 7. Nächste konkrete Schritte

1. **ESP-PROG-2** abwarten → Flash-Test (entscheidet A vs. endgültig B).
2. Material für Weg B beschaffen (3× ESP32-C3 Super Mini, MOSFETs, Kleinteile, Duft-Tabs).
3. Mustergerät umbauen und flashen; `min_power` justieren.
4. `GET /events`-Mitschnitt liefern → Feinschliff App/Firmware.
5. Auf Gerät 2 + 3 übertragen (OTA nach erstem USB-Flash).
6. Betriebskonzept aktivieren (Automatik-Parameter setzen; optional Homey-Szenen).

---

## 8. Referenzen im Repo

| Thema | Datei |
|-------|-------|
| Schnittstellen App ↔ Gerät | `docs/TECHNISCHE_DOKUMENTATION.md` |
| ESPHome-Firmware | `firmware/common/hide-diffuser.yaml`, `firmware/README.md` |
| App-Übersicht | `README.md` |
