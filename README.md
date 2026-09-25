# 🐍 Python IDE & Interpreter for Android

<p align="center">
  <a href="https://github.com/juergen874/Python-IDE-Runner/releases/latest">
    <img src="https://img.shields.io/github/v/release/juergen874/Python-IDE-Runner?color=success&label=Official%20Release&logo=github&style=for-the-badge" alt="Latest Release">
  </a>
  <a href="https://github.com/juergen874/Python-IDE-Runner/releases/download/v1.1/Python-IDE-1.1.apk">
    <img src="https://img.shields.io/badge/Download_APK-v1.1_(81_MB)-blue?logo=android&logoColor=white&style=for-the-badge" alt="Download APK">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_7.0%2B-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Arch-arm64--v8a%20%7C%20x86__64-orange.svg" alt="Arch">
  <img src="https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-Material_3-blue.svg" alt="Compose">
  <img src="https://img.shields.io/badge/Python-3.10_(Chaquopy)-yellow.svg" alt="Python">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey.svg" alt="License">
</p>

---

> [!TIP]
> ### ⚡ Schnelleinstieg & APK-Download
> Du musst die App **nicht selbst kompilieren**. Die installierbare Android-Version steht als Release bereit:
>
> 📥 **[Direkter APK-Download: Python-IDE-1.1.apk](https://github.com/juergen874/Python-IDE-Runner/releases/download/v1.1/Python-IDE-1.1.apk)** *(ca. 81 MB, v1.1)*  
> 🏷️ **[Alle Versionen & Changelogs: GitHub Releases](https://github.com/juergen874/Python-IDE-Runner/releases)**

---

Eine vollwertige, native **Android Python-Entwicklungsumgebung (IDE) und Python-Interpreter**. 

Mit der integrierten **Chaquopy Python 3.10 Engine** führt die App Python-Code nativ und offline direkt auf dem Android-Gerät aus – ganz ohne Cloud-Abhängigkeiten oder Server. Unterstützt werden numerische Berechnungen mit **NumPy**, Datenvisualisierung mit **Matplotlib**, Multithreading, Socket-Netzwerkkommunikation, IoT-Anwendungen sowie interaktive WebViews.

---

## 🌟 Warum diese IDE? (Vorteile)

- 🚀 **Echtes „Zero Setup“**: Kein Linux-Terminal, kein fehleranfälliges `pip install` und keine Build-Probleme auf dem Smartphone. C- und Mathe-Bibliotheken (wie NumPy und Matplotlib) sind bereits vorkompiliert, getestet und sofort startklar.
- 📱 **Touch-optimierte Entwicklung**: Große Schaltflächen (**Run ▶**, **Stop ⏹**, **Save 💾**), Schnellzugriff auf oft benötigte Programmiersymbole (`Tab`, `:`, `()`, `[]`, `def`, `class`, `try`, `while`, `with`) und ein seitlicher Datei-Explorer machen das Coden auf dem Smartphone komfortabel.
- 🖥️ **Clevere 3-Tab-Architektur**: Klare Aufteilung in Vollbild-Editor, ANSI-Streaming-Terminal und visuelle Web-/Plot-Ausgabe – alles lässt sich mit einer Wischgeste oder einem Fingertipp erreichen.
- 📊 **Nahtlose Visualisierung ohne Desktop**: Ruft dein Skript `plt.show()` auf, fängt die IDE das automatisch ab und rendert Grafiken hochauflösend. Lokale HTML/JS-Dashboards und HTTP-Server werden direkt im integrierten WebView interaktiv dargestellt.
- 🛑 **Zuverlässiger Notaus-Schalter (Stop)**: Endlosschleifen oder blockierende Netzwerk-Sockets können per Knopfdruck sauber via `KeyboardInterrupt` abgebrochen werden – ohne dass Android einfriert („App reagiert nicht“).
- 📶 **Voller Zugriff auf das lokale Heimnetzwerk**: Im Gegensatz zu Cloud-Notebooks hat das Smartphone im WLAN direkten Socket-Zugriff auf lokale IP-Adressen (z. B. Smart-Home-Geräte, Mikrocontroller, Sensoren oder Router).
- 🔒 **100 % Offline & Privatsphäre**: Läuft komplett autark auf dem Gerät – keine Cloud, keine Serverkosten, kein Datenabfluss und keine Abhängigkeit von einer Internetverbindung.
- 🛠️ **Maximale Flexibilität**: Anpassungen, neue Berechnungen oder zusätzliche Schnittstellen können direkt auf dem Gerät im Code geändert und sekundenschnell getestet werden – ohne eine ganze Android-App neu kompilieren zu müssen.

---

## 🚀 Releases & Downloads

| Release | Dateiname | Zielsysteme | Download-Link | Hinweise |
| :--- | :--- | :--- | :--- | :--- |
| **v1.1 (Neueste)** | `Python-IDE-1.1.apk` | Android 7.0+ (`arm64-v8a`, `x86_64`) | [👉 **APK v1.1 herunterladen**](https://github.com/juergen874/Python-IDE-Runner/releases/download/v1.1/Python-IDE-1.1.apk) | Erweiterte Editor-Snippets (`try`, `while`, `with`, `self.`), Versions-Footer, CI-Builds |
| **v1.0** | `Python-IDE-1.0.apk` | Android 7.0+ (`arm64-v8a`, `x86_64`) | [📦 **v1.0 herunterladen**](https://github.com/juergen874/Python-IDE-Runner/releases/download/v1.0/Python-IDE-1.0.apk) | Initial Release mit NumPy, Matplotlib & Modbus |
| **Repo Mirror** | `Python-IDE-1.1.apk` | Universell | [📦 Aus Repository laden](apk/Python-IDE-1.1.apk) | Lokales Git-Asset im `apk/`-Verzeichnis |

### 📲 Schritt-für-Schritt-Installation auf Android:
1. Lade die APK über den Link oben herunter: [**`Python-IDE-1.1.apk`**](https://github.com/juergen874/Python-IDE-Runner/releases/download/v1.1/Python-IDE-1.1.apk).
2. Öffne die heruntergeladene Datei (über die Statusleiste oder deinen Dateimanager wie z. B. *Files* oder *Downloads*).
3. Falls Android eine Sicherheitsabfrage anzeigt:
   - Tippe auf **Einstellungen** und aktiviere den Schalter **„Installation aus dieser Quelle zulassen“** (für deinen Browser oder Dateimanager).
4. Bestätige die Installation mit **Installieren**.
5. Starte die App **Python IDE** direkt aus deiner App-Übersicht.

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
| **`pysolarmanv5`** | 3.0.6 | Modbus RTU/TCP-Kommunikation für Hardware- und IoT-Geräte |
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
   - Fragt einen Hybrid-Wechselrichter per Modbus TCP (Port 502) oder WLAN-Stick (Port 8899) im Heimnetzwerk ab.
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
