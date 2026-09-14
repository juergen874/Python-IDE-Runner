# 🐍 Python IDE & Deye SUN-12K Modbus Monitor for Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Python](https://img.shields.io/badge/Python-3.10%20(Chaquopy)-yellow.svg)](https://chaquo.com/chaquopy/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Eine vollwertige, native **Android Python-Entwicklungsumgebung (IDE)** und **Live-Telemetrie-Monitor für Deye SUN-12K Hybrid-Wechselrichter** (`SUN-12K-SG04LP3`). 

Angetrieben von der **Chaquopy Python 3.10 Engine**, bietet die App eine nahtlose Ausführung von Python-Skripten direkt auf dem Android-Gerät – inklusive NumPy, Matplotlib, reiner Socket-Kommunikation und interaktiver WebView-Dashboards.

---

## ✨ Highlights & Features

### 🖥️ 1. Vollbild Drei-Tab-Architektur
Die Benutzeroberfläche nutzt das gesamte Display optimal über dedizierte Vollbild-Tabs:
- **📝 Tab 1: Editor**
  - Vollbild-Code-Editor mit Zeilennummern und dynamischem Syntax-Highlighting.
  - Schnelleingabeleiste mit 1-Tap-Einrückung (`Tab` / 4 Leerzeichen) sowie Programmiersymbolen (`def`, `class`, `import`, `print`, `:`, `()`, `[]`, `{}`).
  - Statusindikator für ungespeicherte Änderungen (`●`).
- **💻 Tab 2: Terminal**
  - Vollbild-Streaming-Konsole für `stdout` und `stderr` in Echtzeit.
  - ANSI-Farbdekodierung, Laufzeitmessung (ms/s), Autoscroll-Schalter und Copy-to-Clipboard.
  - Automatischer Fokus auf das Terminal beim Starten eines Skripts.
- **🌐 Tab 3: Web / Visual View**
  - Interaktives Rendern von generierten HTML/CSS/JavaScript-Dashboards und Matplotlib-Diagrammen.
  - Direkte Anzeige des lokalen Deye Live-Dashboards (`http://localhost:8080`).

---

### ⚡ 2. Deye SUN-12K Hybrid-Wechselrichter Integration
Enthält ein optimiertes, robustes Python-Skript (`deye_12k_inverter.py`) zur Auslesung von Deye Hybrid-Wechselrichtern über das Heimnetzwerk:
- **Dual-Modus**:
  - **Solarman V5 WLAN-Stick** (Port `8899`) über hardwarenahe Frame-Zusammensetzung.
  - **Modbus TCP Gateway / Waveshare RS485-zu-Ethernet** (Port `502`).
- **Stabilität & Timing**:
  - Pufferpausen von 80 ms (`INTER_BLOCK_DELAY`) zwischen Modbus-Registerblöcken gegen Paketverluste im Logger-Stick.
  - `read_holding_registers_safe` mit Wiederholungsversuchen und 500 ms Reconnect-Puffer.
  - Sauberes Socket-Closing (`socket.shutdown(socket.SHUT_RDWR)`) zur Vermeidung blockierter Ports.
- **Plausibilitäts-Check**:
  - Automatische Leistungsberechnung (`P = U × I`), falls Register 590 `0 W` meldet, aber Ladestrom fließt.
- **Umfangreiche Telemetrie**:
  - PV-Leistung (String 1 & 2, Gesamtleistung).
  - Batterie: SoC (%), Spannung, Strom, Lade-/Entladeleistung, Temperatur.
  - Hausverbrauch & Lastverteilung je Phase (L1, L2, L3).
  - Netzeinspeisung / Netzbezug & Netzfrequenz.
  - Tageserträge (PV, Grid Buy/Sell, Battery Charge/Discharge).
  - Integrierter lokaler Webserver mit ansprechendem HTML5-Live-Dashboard.

---

### 📂 3. Workspace & Dateimanager
- Dateiverwaltung über das Slide-Out-Menü (Drawer):
  - Erstellen (`.py`, `.json`, `.html`, `.txt`).
  - Umbenennen, Speichern und Löschen.
  - Vorinstallierte Beispielskripte für den Schnellstart.

---

## 📦 Vorinstallierte Python-Pakete & Beispiele

Die App integriert folgende Python-Pakete direkt über Chaquopy:
- **`numpy`**: Numerische Berechnungen und Matrix-Operationen.
- **`matplotlib`**: Automatische Konvertierung von Plots in Base64 zur direkten Visualisierung.
- **`pysolarmanv5`**: Referenzbibliothek für Solarman V5 Datenlogger.
- **Standardbibliothek**: `socket`, `struct`, `http.server`, `threading`, `json`, `math`, etc.

### Enthaltene Beispielskripte:
1. `deye_12k_inverter.py` – Deye Hybrid-Wechselrichter Modbus Reader & Web-Dashboard.
2. `1_matplotlib_waves.py` – Generierung und Anzeige wissenschaftlicher Plots.
3. `2_numpy_matrix.py` – Matrixberechnungen und Eigenwert-Analysen.
4. `3_interactive_html_dashboard.py` – Generierung von interaktiven HTML/SVG-Dashboards.

---

## 🚀 Installation & Bauen

### Voraussetzungen
- **Android Studio** (Koala / Ladybug oder neuer empfohlen)
- **JDK 17**
- **Android SDK** mit Minimum SDK 24 (Android 7.0+) und Target SDK 36

### Aus dem Quellcode kompilieren
1. Repository klonen:
   ```bash
   git clone https://github.com/your-username/python-ide-deye-android.git
   cd python-ide-deye-android
   ```
2. Projekt in Android Studio öffnen oder per Gradle kompilieren:
   ```bash
   gradle assembleDebug
   ```
3. Die fertige APK auf dem Android-Gerät installieren:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Deye Konfiguration anpassen

Öffne in der App die Datei `deye_12k_inverter.py` im **Editor-Tab** und passe die Kopfzeilen an deine Anlage an:

```python
# ==============================================================================
# ⚙️ DEYE WECHSELRICHTER & LOGGER KONFIGURATION
# ==============================================================================
INVERTER_IP = "192.168.188.128"   # IP-Adresse des Deye Inverters / WLAN-Sticks
INVERTER_PORT = 8899              # 8899 (Solarman V5 WLAN Stick) oder 502 (Modbus TCP RS485)
LOGGER_SERIAL = 1109501211        # ⚠️ 10-stellige Seriennummer vom Aufkleber des WLAN-Sticks
SLAVE_ID = 1                      # Modbus Slave ID (Standard: 1)
POLL_INTERVAL = 4                 # Abfrageintervall in Sekunden
WEB_PORT = 8080                   # Port für das Web-Dashboard
# ==============================================================================
```

- **Seriennummer**: Die 10-stellige Seriennummer findest du direkt auf dem Barcode-Aufkleber des Deye WLAN-Sticks.
- Nach dem Anpassen einfach auf **Speichern** und anschließend auf **Run ▶** tippen.
- Im **Terminal-Tab** siehst du die Live-Logs.
- Im **Web / Visual View Tab** erscheint das Live-Dashboard!

---

## 🛠️ Technische Details

| Komponente | Technologie |
|---|---|
| **Programmiersprache** | Kotlin 2.0+ & Python 3.10 |
| **UI Framework** | Jetpack Compose mit Material Design 3 (M3) |
| **Python Bridge** | Chaquopy 15.0+ |
| **Concurrency** | Kotlin Coroutines & `StateFlow` |
| **Netzwerk-Sicherheit** | `usesCleartextTraffic="true"` (für lokales Dashboard auf `localhost:8080`) |
| **Berechtigungen** | `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE` |

---

## 📄 Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).
