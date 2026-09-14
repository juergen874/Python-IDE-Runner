# 🐍 Python IDE & Interpreter for Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Python](https://img.shields.io/badge/Python-3.10%20(Chaquopy)-yellow.svg)](https://chaquo.com/chaquopy/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Eine vollwertige, native **Android Python-Entwicklungsumgebung (IDE) und Python-Interpreter**. 

Mit der integrierten **Chaquopy Python 3.10 Engine** führt die App Python-Code nativ und offline direkt auf dem Android-Gerät aus – ganz ohne Cloud-Abhängigkeiten oder Server. Unterstützt werden numerische Berechnungen mit **NumPy**, Datenvisualisierung mit **Matplotlib**, Multithreading, Socket-Netzwerkkommunikation sowie interaktive WebViews.

---

## ✨ Features & Funktionsumfang

### 🖥️ 1. Vollbild Drei-Tab-Benutzeroberfläche
Jede Hauptansicht verfügt über die volle Bildschirmhöhe und -breite:

- **📝 Tab 1: Editor**
  - Vollbild-Code-Editor mit Zeilennummern und dynamischem Syntax-Highlighting.
  - Quick-Access-Symbolleiste für schnelles Programmieren auf Touchscreens:
    - `Tab`-Taste für 4-Leerzeichen-Einrückung.
    - Schnelleingaben für häufige Python-Tokens (`def`, `class`, `import`, `print`, `:`, `()`, `[]`, `{}`).
  - Statusanzeige für ungespeicherte Änderungen (`●`).

- **💻 Tab 2: Terminal**
  - Vollbild-Streaming-Konsole für `stdout` und `stderr` in Echtzeit.
  - Automatische ANSI-Farbdarstellung (z. B. für formatierte Status- oder Fehlerausgaben).
  - Ausführungszeit-Messung (ms / s).
  - Autoscroll-Schalter, Copy-to-Clipboard und Clear-Funktion.
  - Beim Klick auf **Run ▶** wechselt die Ansicht automatisch in das Terminal.

- **🌐 Tab 3: Web / Visual View**
  - Integrierte visuelle Ausgabe für grafische Ergebnisse:
    - **Matplotlib Plots**: Automatisch erfasste Diagramme werden gerendert.
    - **Web-Dashboards**: Generierte HTML/CSS/JavaScript-Dateien oder lokale HTTP-Server (z. B. `http://localhost:8080`) werden interaktiv dargestellt.
  - Badge-Indikator, sobald neue Visualisierungen verfügbar sind.

---

### 📊 2. WebView- & Visual-Output-Abfangen
- **Ausgabe-Erkennung**: Wenn ein Skript HTML generiert oder Matplotlib-Plots erzeugt, wird dies automatisch abgefangen und bereitgestellt.
- **Matplotlib-Hook**: Automatische Konvertierung von `plt.show()` in Base64/PNG oder HTML zur direkten Darstellung ohne Desktop-Fenstermanager.
- **Integrierte Android WebView-Komponente**: Um HTML-Inhalte, Grafiken oder Web-Visualisierungen interaktiv darzustellen (unterstützt auch lokale HTTP-Server wie `localhost:8080`).

---

### 📦 3. Enthaltene Python-Laufzeit & Pakete

Die IDE liefert eine vorkonfigurierte Python 3.10 Umgebung mit folgenden Bibliotheken:
- **`numpy`**: Vektor- und Matrixberechnungen, lineare Algebra.
- **`matplotlib`**: 2D/3D-Plots (wird automatisch als Bild im Visual View Tab dargestellt).
- **Python Standard Library**: `socket`, `threading`, `http.server`, `json`, `math`, `struct`, `urllib`, etc.
- **`pysolarmanv5`**: Vorkonfiguriertes Paket für Modbus/Solarman-Kommunikation.

---

### 📂 4. Workspace- & Dateimanager
- Slide-Out Drawer zur Verwaltung eigener Python-Skripte im App-Workspace:
  - Neue Dateien anlegen (`.py`, `.json`, `.html`, `.txt`).
  - Dateien umbenennen, speichern und löschen.
  - Automatische Sicherung und Wiederherstellung des Arbeitsbereichs.

---

## 💡 Enthaltene Beispielskripte & Showcase

Die IDE enthält verschiedene Beispielskripte, die das breite Einsatzspektrum demonstrieren:

1. **`1_matplotlib_waves.py`** – Wissenschaftliche Visualisierung
   - Berechnet Sinus-/Kosinuswellen mit NumPy und stellt sie mit Matplotlib dar.
   - Der Plot erscheint direkt im **Web / Visual View Tab**.

2. **`2_numpy_matrix.py`** – Numerische Mathematik
   - Matrix-Multiplikation, Determinanten- und Eigenwertberechnungen auf der CPU des Smartphones.

3. **`3_interactive_html_dashboard.py`** – Web-Generierung
   - Erzeugt ein interaktives HTML5/SVG-Dashboard, das direkt im WebView gerendert wird.

4. **`deye_12k_inverter.py`** – IoT & Hardware-Kommunikation (Real-World Showcase)
   - Beispiel für hardwarenahe Netzwerkkommunikation via reine Python-Sockets.
   - Fragt einen Deye Hybrid-Wechselrichter per Modbus TCP (Port 502) oder Solarman V5 WLAN-Stick (Port 8899) im Heimnetzwerk ab.
   - Startet einen lokalen HTTP-Server im Hintergrund-Thread und streamt Telemetriedaten live in den **Web / Visual View Tab**.

---

## 🚀 Installation & Bauen

### Voraussetzungen
- **Android Studio** (Koala / Ladybug oder neuer)
- **JDK 17**
- **Android SDK** (Min SDK: 24, Target SDK: 36)

### Kompilieren
1. Repository klonen:
   ```bash
   git clone https://github.com/your-username/python-ide-android.git
   cd python-ide-android
   ```
2. Projekt über Gradle bauen:
   ```bash
   gradle assembleDebug
   ```
3. APK auf dem Smartphone / Emulator installieren:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🛠️ Technische Architektur

| Schicht | Technologie | Beschreibung |
|---|---|---|
| **UI & Presentation** | Jetpack Compose | Material Design 3, reaktive Zustände via `StateFlow` |
| **Interpreter Core** | Chaquopy (Python 3.10) | Nativer In-Process Python-Interpreter für Android |
| **Output Interceptor** | Standard Streams Redirect | Leitet Python `sys.stdout` & `sys.stderr` in Echtzeit an Compose weiter |
| **Visual Bridge** | Base64 Image & WebView | Fängt Matplotlib-Figuren ab und stellt HTML/Webserver dar |
| **Berechtigungen** | `INTERNET`, `ACCESS_NETWORK_STATE` | Erlaubt Netzwerk-Sockets (z. B. für IoT, APIs oder lokale Webserver) |

---

## 📄 Lizenz

Dieses Projekt ist unter der [MIT-Lizenz](LICENSE) lizenziert.
