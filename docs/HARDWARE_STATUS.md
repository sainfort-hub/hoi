# Hardware-Status & Handoff — Revoltab HIDE / SantaTAB

Lebendes Statusdokument für die Hardware- und Reverse-Engineering-Seite. Ergänzt den RE-Bericht «Revoltab HIDE — Projekt-Zämmefassig» und das «Technische Konzept». Software-/App-Details: siehe `TECHNISCHE_DOKUMENTATION.md`.

**Stand: 01.09.2026 · Zielarchitektur Weg B (Hardware-Ersatz) — endgültig.**

---

## 1. Kurzstatus (Ampel)

| Bereich | Stand | Bemerkung |
|---|---|---|
| Analyse / RE des Originals | 🟢 abgeschlossen | Cloud tot, Flash-CA blockiert, UART gesperrt, **JTAG gesperrt** |
| Flash-Zugriff (Weg A) | 🔴 endgültig blockiert | Chip verweigert Debug-Zugriff, siehe Abschnitt 2 |
| Zielentscheid | 🟢 Weg B, endgültig | Logikplatine ersetzen (ESP32-C3 + ESPHome) |
| App SantaTAB | 🟢 gebaut & baubar | MQTT- und Direkt-Modus, CI grün |
| ESPHome-Firmware | 🟡 auf XIAO angepasst, ungetestet | `seeed_xiao_esp32c3`, PWM 20 kHz, Modul-Verdrahtung (01.09.2026) |
| Material Weg B | 🟡 bestellt 01.09.2026 | Bastelgarage, Lieferung ca. 03.09.2026 |
| Hardware-Umbau | 🔴 offen | Mustergerät nach Materialeingang |
| Verifikation am Gerät | 🔴 offen | REST/SSE, PWM-min_power, SNTP-Taktung prüfen |

Legende: 🟢 erledigt · 🟡 bereit, aber zu verifizieren · 🔴 offen.

---

## 2. Was aus dem Reverse Engineering feststeht

### 2.1 Cloud und Netzwerk (Stand vor 01.09.)
- **Cloud tot:** Afero-Tenant `o80y8sax` — DNS gelöscht, kein Original-Betrieb möglich.
- **Netzwerk-Weg blockiert:** DNS-Redirect + TLS gehen, Handshake scheitert an `UNKNOWN_CA` (Gerät prüft gegen Flash-CA / Afero-Signatur).
- **UART-Flash gesperrt:** eFuse `UART_DOWNLOAD_DIS` gesetzt → kein Reflash über UART.

### 2.2 JTAG-Test (23.08.–01.09.2026, ESP-PROG-2) — negativ, abgeschlossen
- Beide JTAG-TAPs antworten (IDCODE 0x120034e5), cpu0-Examination erfolgreich. Verkabelung ist bewiesen sauber (TDI-Durchgang über Debug-Register bestätigt).
- **Jede Debug-Instruktion wirft eine Exception** (DSR 8020CC13, «Exception reading expstate»). Flasher-Stub wird geladen, läuft aber nie fertig (PC 0x400BD010, -302).
- `reset halt` per JTAG landet auf PC 0x50000004 statt am Reset-Vektor 0x40000400.
- Watchdog-Kill und VDD_SDIO-Force per `mww` kommen nie zur Ausführung, weil OpenOCD nach dem gescheiterten Halt abbricht.
- **Schlussfolgerung:** Der Chip lässt Debug-Zugriff nicht zu (mit hoher Wahrscheinlichkeit JTAG-Disable-eFuse aus dem Afero-Secure-Build). Weg A ist damit geschlossen. Chip-off-Auslesen der Flash wäre technisch möglich, lohnt sich aber nicht: Flash-Encryption wahrscheinlich, und der ESP32 steuert in der Afero-Architektur ohnehin nur die WLAN-Brücke.

### 2.3 Original-Firmware, beobachtet über UART (01.09.2026)
- Bootbanner: `Copyright 2016-2020 Afero, Inc.` — ESP32 läuft Afero-Hub-Firmware (ESP-IDF-basiert), Versionen `2.3.5.23261, 2.3.4.76, 2.3.4.60`.
- Geräte-IDs: `8455ece334aaebb6`, `80268c5006d7ece334aaebb6`, `5a`.
- Cloud-Endpunkte: `echo1.o80y8sax.afero.net`, `conclave-stream1.o80y8sax.afero.net`; Zeitquellen `pool.ntp.org`, `time.nist.gov`. Alle DNS-Anfragen laufen in Timeout.
- Firmware stürzt nach DNS-Fehlern mit `Guru Meditation Error (StoreProhibited)` ab und rebootet (Endlosschleife).
- Bei Speisung über ESP-PROG-2 (3V3) wiederholt `Brownout detector was triggered` beim WLAN-Start — Prog-2 reicht nur für den ROM-Zustand, nicht für laufende Firmware.
- Bei IO0 = GND: ROM meldet zyklisch `Download mode is disabled. Restart with GPIO0 high` (RTC-Watchdog-Reset-Loop im ROM).

### 2.4 Hardware-Fakten
- **Architektur:** ESP32-WROOM-32UE (WLAN-Hub) + nRF52832 (Afero Secure Radio, ASR-2) + ST CR95HF (NFC, Kapselerkennung). Logikplatine `RevoltabHide_RevV3.1 2025_06`.
- **Gerät ist simpel:** Der «Diffusor» ist nur ein 5-V-Lüfter (SUNON UB5U3-500, 72 mA). Keine Heizung, kein Piezo. «Intensität» = Lüfterdrehzahl bzw. Laufzeit.
- **Netzteilplatine ist nicht galvanisch getrennt** (bestätigt: Brückengleichrichter, 400-V-Elko, Drosseln, kein Trafo). GND der Niederspannungsseite liegt auf Netzpotential.
- **Lüfter** hängt an J6 der Netzteilplatine (Rot = +5 V, Schwarz = GND, Gelb = Tacho/PWM). J7 (Grün/Weiss) = zweites Element im Lüfterkopf, vermutlich LED oder Kapselsensor — für Weg B nicht benötigt.
- **5-V-Schiene:** vermutlich an den zwei gelb abgeklebten Elkos der Netzteilplatine; per Messung zu bestätigen.
- **Flex-Verbindung** Netzteilplatine ↔ Logikplatine ist beidseitig gelötet (kein Stecker) → wird durchtrennt.
- **SW1 (Taster) und DL2 (LED)** sitzen auf der Rückseite der Netzteilplatine und können optional für GPIO5/GPIO6 weiterverwendet werden.
- **Antenne:** u.FL-Koaxantenne des WROOM-32UE wird auf dem XIAO weiterverwendet.

Testpunkt-Mapping der Logikplatine (per Durchgang verifiziert, nur noch für die Akten): TP3 = Pad 13 (GPIO14/TMS), TP4 = Pad 16 (GPIO13/TCK), TP6 = Pad 14 (GPIO12/TDI), Pad 23 = GPIO15/TDO, TP12 = TXD0, TP13 = RXD0, TP7 = EN. Header «JTAG NRF» Pin 1 = 3V3, Pin 5 = GND. Header «JTAG ESP» ist in Wirklichkeit ein Programmier-Header (3V3/EN/RXD0/GND), kein JTAG. Die frühere Tabelle (TP4=TMS, TP6=TCK, TP3=TDI, TP8=TDO) war falsch.

➡️ **Konsequenz:** Weg B (eigener ESP32-C3 statt Originalplatine) ist der einzige Weg und entfernt zugleich die NFC-Kapselsperre.

---

## 3. Material Weg B (bestellt 01.09.2026, Bastelgarage)

| Menge | Artikel | Art.-Nr. | Verwendung |
|---|---|---|---|
| 3 | Seeed Studio XIAO ESP32-C3 | 422551 | Controller (ersetzt Super Mini aus dem Konzept) |
| 3 | 15A 400W MOSFET Treiber 5-36V DC (AOD4184, D4184-Modul) | 420985 | Lüfter-Treiberstufe (ersetzt diskreten AO3400A) |
| 1 | Elektrolyt Kondensator Set 120 Stk. | 420277 | 470 µF / 16 V am 5V-Pin des XIAO |
| 1 | Dioden Set 100 Stk. | 420362 | 1N5819 Freilaufdiode |
| 1 | Flachkabel IDC 40p 28AWG | 421016 | Verdrahtung |

Vorhanden: ESP32-C6-DevKitM-1 (Tischaufbau/Test), u.FL-Antennen aus den drei Originalgeräten.

**Abweichungen zum Konzept 4.3:** Das MOSFET-Modul bringt Gate-Widerstand, Pulldown und Schraubklemmen mit — 100 Ω und 10 kΩ entfallen. XIAO statt Super Mini: GPIO-Nummern bleiben identisch (GPIO4 = D2, GPIO5 = D3, GPIO6 = D4).

---

## 4. Umbau pro Gerät (Weg B) — Checkliste

Pro Diffuser (3 Geräte im Eigengebrauch):

- ☐ Gerät spannungsfrei schalten (230 V!) und öffnen
- ☐ Messung vor dem Umbau (Netzteilplatine an 230 V, sonst nichts angeschlossen, Multimeter): J6 Rot–Schwarz, Plus der gelben Elkos gegen J6 Schwarz → 5-V-Abgriff festlegen
- ☐ Flex zwischen Netzteilplatine und Logikplatine durchtrennen; Logikplatine ausbauen, u.FL-Antenne abnehmen
- ☐ XIAO ESP32-C3 **stromlos per USB** mit ESPHome flashen (erstes Mal), danach nur OTA
- ☐ MOSFET-Modul und XIAO verdrahten (siehe unten), 470-µF-Elko an 5V/GND des XIAO, 1N5819 über den Lüfter
- ☐ Antenne auf XIAO stecken
- ☐ `min_power` empirisch justieren (Lüfter läuft gerade noch zuverlässig an)
- ☐ Einbau, Isolation zur 230-V-Seite prüfen, Dauerlauf beobachten

### Verdrahtung mit MOSFET-Modul

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

⚠️ **Sicherheit:** Netzteil ist **nicht** galvanisch getrennt (bestätigt). Sobald 230 V anliegen: kein USB, kein Programmieradapter, kein PC am Gerät, nicht anfassen. Messen nur mit batteriebetriebenem Multimeter. Flashen ausschliesslich stromlos per USB, im Betrieb nur OTA. Feste Elektroinstallation in der CH ist bewilligungspflichtig.

---

## 5. Firmware-Stand (ESPHome)

Quelle: `firmware/` (gemeinsames Paket `common/hide-diffuser.yaml` + 3 Gerätedateien).

Bereits enthalten:
- `fan` (speed, 10 Stufen) an GPIO4 (LEDC-PWM, `min_power: 0.25`)
- MQTT mit Discovery (App-MQTT-Modus + Homey), `reboot_timeout: 0s`
- `web_server` (REST + SSE, Port 80, `version: 2`, `local: true`) — App-Direkt-Modus
- SNTP-Zeit + autonome stündliche Taktung (Nachtruhe 22–6 Uhr)
- Persistente Parameter: Automatik (switch), Grundstufe (1–10), Sekunden pro Stunde (0–3600)
- optional Taster (GPIO5) und Status-LED (GPIO6)

**Angepasst am 01.09.2026 (XIAO) — in `common/hide-diffuser.yaml` umgesetzt:**
1. ✅ `board: seeed_xiao_esp32c3`
2. ✅ PWM-Frequenz 25 kHz → **20 kHz** (Spec-Grenze des D4184-Moduls)
3. ✅ GPIO4/5/6 unverändert (XIAO D2/D3/D4); Verdrahtungskommentar auf MOSFET-Modul aktualisiert

Eine Firmware bedient beide App-Modi (mit oder ohne Broker).

---

## 6. Am echten Gerät zu verifizieren

1. **PWM-Anlaufschwelle:** `min_power` so weit senken, bis der Lüfter aus dem Stand zuverlässig anläuft (Startwert 0.25).
2. **REST-Parametername:** ob `POST /fan/<obj>/turn_on?speed_level=<n>` die Stufe korrekt setzt.
3. **SSE-JSON-Felder:** `GET /events` → Felder `id`, `state`, `speed_level`, `speed_count`, `name` gegenprüfen.
4. **MQTT-Discovery:** ob die Fan-Entität `percentage_*`-Topics mit `speed_range` 1..10 liefert.
5. **SNTP:** Zeitquelle (Internet oder lokaler NTP) — sonst läuft die autonome Taktung nicht.
6. **Speisung:** XIAO bootet stabil beim WLAN-Start ab 5-V-Schiene der Netzteilplatine (kein Brownout).

➡️ Sobald ein Gerät läuft: `GET /events`-Mitschnitt + Beobachtungen gemäss Eingabevorlage (Abschnitt 7 der technischen Doku) liefern.

---

## 7. Weg A — geschlossen

Weg A (Original-Firmware erhalten, Flash-CA ersetzen) ist nach dem JTAG-Ergebnis vom 01.09.2026 nicht mehr verfolgbar: kein Debug-Zugriff, kein UART-Download, Flash-Encryption wahrscheinlich. Selbst ein gelungener Chip-off-Dump würde nur die WLAN-Brücke betreffen, nicht die Gerätelogik im nRF52832. Kein weiterer Aufwand.

---

## 8. Nächste konkrete Schritte

1. Materialeingang abwarten (ca. 03.09.2026).
2. Messung 5-V-Abgriff an der Netzteilplatine (Abschnitt 4, Punkt 2).
3. ✅ `hide-diffuser.yaml` auf XIAO angepasst (Abschnitt 5) → Mustergerät flashen.
4. Mustergerät umbauen, `min_power` justieren, Dauerlauf.
5. `GET /events`-Mitschnitt liefern → Feinschliff App/Firmware.
6. Auf Gerät 2 + 3 übertragen (OTA nach erstem USB-Flash).
7. Betriebskonzept aktivieren (Automatik-Parameter setzen; optional Homey-Szenen).

---

## 9. Referenzen im Repo

| Thema | Datei |
|---|---|
| Schnittstellen App ↔ Gerät | `docs/TECHNISCHE_DOKUMENTATION.md` |
| ESPHome-Firmware | `firmware/common/hide-diffuser.yaml`, `firmware/README.md` |
| App-Übersicht | `README.md` |
