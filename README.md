# 🐍 Python IDE & Interpreter for Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0-brightgreen.svg)](apk/Python-IDE-1.0.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Python](https://img.shields.io/badge/Python-3.10%20(Chaquopy)-yellow.svg)](https://chaquo.com/chaquopy/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Eine vollwertige, native **Android Python-Entwicklungsumgebung (IDE) und Python-Interpreter**. 

Mit der integrierten **Chaquopy Python 3.10 Engine** führt die App Python-Code nativ und offline direkt auf dem Android-Gerät aus – ganz ohne Cloud-Abhängigkeiten oder Server. Unterstützt werden numerische Berechnungen mit **NumPy**, Datenvisualisierung mit **Matplotlib**, Multithreading, Socket-Netzwerkkommunikation, IoT-Anwendungen (z. B. Solarman/Modbus für Photovoltaik-Wechselrichter) sowie interaktive WebViews.

---

## 📥 Download & Installation (APK)

Die fertige Android-App kann sofort als APK heruntergeladen und auf dem Android-Gerät installiert werden:

- 📦 **Direkter Download im Repository**: [**`apk/Python-IDE-1.0.apk`**](apk/Python-IDE-1.0.apk) (Version 1.0, ~81 MB)
- 🚀 **GitHub Releases**: [**Releases v1.0**](https://github.com/juergen874/Python-IDE-Runner/releases)

### 📲 Schritt-für-Schritt-Installation:
1. Lade die APK-Datei ([`Python-IDE-1.0.apk`](apk/Python-IDE-1.0.apk)) auf dein Android-Gerät herunter.
2. Öffne die heruntergeladene Datei über die Benachrichtigungsleiste oder deinen Dateimanager.
3. Falls gefragt: **"Installation aus unbekannten Quellen"** für den Browser bzw. Dateimanager in den Android-Einstellungen erlauben.
4. Auf **Installieren** tippen und die App öffnen.

---

## ✨ Features & Funktionsumfang

### 🖥️ 1. Vollbild Drei-Tab-Benutzeroberfläche
Jede Hauptansicht nutzt die volle Bildschirmhöhe und -breite optimal aus:

- **📝 Tab 1: Editor**
  - Vollbild-Code-Editor mit Zeilennummern und dynamischem Syntax-Highlighting (`PythonSyntaxHighlighter`).
  - Quick-Access-Symbolleiste für schnelles Programmieren auf Touchscreens:
    - `Tab`-Taste für standardmäßige 4-Leerzeichen-Einrückung.
    - Schnelleingaben für häufige Python-Tokens (`def`, `class`, `import`, `print`, `:`, `()`, `[]`, `{}`).
  - Statusanzeige für ungespeicherte Änderungen (`●`).
  - Zoom & Schriftgrößenanpassung.

- **💻 Tab 2: Terminal**
  - Vollbild-Streaming-Konsole für `stdout` und `stderr` in Echtzeit.
  - Automatische ANSI-Farbdarstellung via `AnsiParser` (z. B. farbige Status- oder Fehlerausgaben).
  - Ausführungszeit-Messung in Millisekunden (`elapsedMs`).
  - Autoscroll-Schalter, Copy-to-Clipboard und Clear-Funktion.
  - Stop-Schalter zur sauberen Unterbrechung laufender Skripte über `KeyboardInterrupt`.
  - Beim Klick auf **Run ▶** wechselt die Ansicht automatisch in das Terminal.

- **🌐 Tab 3: Web / Visual View**
  - Integrierte visuelle Ausgabe für grafische Ergebnisse:
    - **Matplotlib Plots**: Automatisch erfasste Diagramme werden nahtlos hochauflösend gerendert.
    - **Web-Dashboards**: Generierte HTML/CSS/JavaScript-Dateien oder lokale HTTP-Server (z. B. `http://localhost:8080`) werden interaktiv im WebView dargestellt.
  - Badge-Indikator, sobald neue Visualisierungen verfügbar sind.

---

### 📊 2. WebView- & Visual-Output-Abfangen
- **Ausgabe-Erkennung**: Wenn ein Skript HTML generiert oder Matplotlib-Plots erzeugt, wird dies automatisch abgefangen und bereitgestellt.
- **Matplotlib-Hook (`runner_hook.py`)**: Fängt `plt.show()` im Hintergrund ab, konvertiert Plots in In-Memory Base64-PNGs und leitet sie direkt an die Android-Ansicht weiter – ganz ohne Desktop-Display-Server.
- **Integrierte Android WebView-Komponente**: Stellt interaktive HTML-Inhalte, SVG-Grafiken oder lokale Dashboards dar (unterstützt auch lokale Server auf Ports wie `8080`).

---

### 📦 3. Enthaltene Python-Laufzeit & Pakete

Die IDE liefert eine vollständige, vorkonfigurierte Python 3.10 Umgebung mit folgenden nützlichen Paketen aus:

| Paket | Version | Verwendungszweck |
|---|---|---|
| **`numpy`** | 1.26.2 | Schnelle Vektor- und Matrixberechnungen, lineare Algebra |
| **`matplotlib`** | 3.6.0 | 2D/3D-Plots (wird automatisch im Visual View gerendert) |
| **`pillow`** | 11.0.0 | Bildverarbeitung und Bildmanipulation |
| **`pysolarmanv5`** | 3.0.6 | Modbus RTU/TCP-Kommunikation mit Solarman / Deye Wechselrichtern |
| **`uModbus`** | 1.0.4 | Modbus-Protokoll-Implementierung |
| **`pyserial`** | 3.5 | Serielle Kommunikation |
| **Python Standard Library** | 3.10 | `socket`, `threading`, `http.server`, `json`, `math`, `struct`, `urllib`, `ctypes`, etc. |

---

### 📂 4. Workspace- & Dateimanager
- Slide-Out Drawer zur Verwaltung eigener Python-Skripte im App-Workspace:
  - Neue Dateien anlegen (`.py`, `.json`, `.html`, `.txt`).
  - Dateien umbenennen, speichern und löschen.
  - Reset-Funktion zur Wiederherstellung aller mitgelieferten Musterskripte.

---

## 💡 Enthaltene Beispielskripte & Showcase

Die IDE enthält verschiedene Beispielskripte, die das breite Einsatzspektrum demonstrieren:

1. **`1_matplotlib_waves.py`** – Wissenschaftliche Visualisierung
   - Berechnet Sinus- und Kosinuswellen mit NumPy und formatiert sie im Dark-Theme.
   - Der Plot erscheint direkt im **Web / Visual View Tab**.

2. **`2_numpy_matrix.py`** – Numerische Mathematik
   - Matrix-Multiplikation, Determinanten- und Inversionsberechnungen nativ auf dem Smartphone.

3. **`3_interactive_html_dashboard.py`** – Web-Generierung
   - Erzeugt ein interaktives HTML5/SVG-Dashboard, das direkt im WebView gerendert wird.

4. **`deye_12k_inverter.py`** – IoT & Photovoltaik (Real-World Showcase)
   - Beispiel für hardwarenahe Netzwerkkommunikation via reine Python-Sockets.
   - Fragt einen Deye Hybrid-Wechselrichter per Modbus TCP (Port 502) oder Solarman V5 WLAN-Stick (Port 8899) im Heimnetzwerk ab.
   - Startet einen lokalen HTTP-Server im Hintergrund-Thread und streamt Telemetriedaten live in den **Web / Visual View Tab**.

---

## 🚀 Selbst kompilieren (Optional)

Wer die App selbst aus dem Quellcode bauen möchte:

### Voraussetzungen
- **Android Studio** (Koala / Ladybug oder neuer)
- **JDK 17**
- **Android SDK** (Min SDK: 24, Target SDK: 36)

### Kompilieren & Installieren
1. Repository klonen:
   ```bash
   git clone https://github.com/juergen874/Python-IDE-Runner.git
   cd Python-IDE-Runner
   ```
2. Projekt über Gradle bauen:
   ```bash
   ./gradlew assembleDebug
   ```
3. APK auf dem Smartphone / Emulator installieren:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🛠️ Technische Architektur

| Schicht | Technologie | Beschreibung |
|---|---|---|
| **UI & Presentation** | Jetpack Compose (Material 3) | Reaktive Zustände via `StateFlow` im `MainViewModel` |
| **Interpreter Core** | Chaquopy (Python 3.10) | Nativer In-Process Python-Interpreter für Android |
| **Output Interceptor** | `runner_hook.py` | Leitet Python `sys.stdout` & `sys.stderr` in Echtzeit an Compose weiter |
| **Visual Bridge** | Base64 Image & WebView | Fängt Matplotlib-Figuren ab und stellt HTML/Webserver dar |
| **Berechtigungen** | `INTERNET`, `ACCESS_NETWORK_STATE` | Erlaubt Netzwerk-Sockets (z. B. für IoT, APIs oder lokale Webserver) |

---

## 📄 Lizenz

Dieses Projekt ist unter der [MIT-Lizenz](LICENSE) lizenziert.
