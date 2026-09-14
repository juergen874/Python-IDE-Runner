package com.example.data

private const val TQ = "\"\"\""

object SampleScripts {

    val DEYE_SCRIPT = """#!/usr/bin/env python3
# Deye SUN-12K Hybrid Inverter (SUN-12K-SG04LP3) Modbus Reader & Web Dashboard
# Robustes Modbus-Timing, Block-Validierung (kein Index-Out-of-Range) & Batterie-Fallback-Berechnung

# ==============================================================================
# ⚙️ DEYE WECHSELRICHTER & LOGGER KONFIGURATION
# ==============================================================================
INVERTER_IP = "192.168.188.128"   # IP-Adresse des Deye Inverters / WLAN-Sticks
INVERTER_PORT = 8899              # 8899 (Solarman V5 WLAN Stick) oder 502 (Modbus TCP RS485-Gateway)
LOGGER_SERIAL = 1109501211        # ⚠️ 10-stellige Seriennummer vom Aufkleber des WLAN-Sticks
SLAVE_ID = 1                      # Modbus Slave ID (Standard: 1)
POLL_INTERVAL = 4                 # Abfrageintervall in Sekunden (4s für stabilen Solarman-Puffer)
WEB_PORT = 8080                   # Port für das Web-Dashboard
# ==============================================================================

import socket
import socketserver
import struct
import sys
import time
import json
import argparse
import urllib.request
import base64
import re
import warnings
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

warnings.filterwarnings("ignore")

def crc16_modbus(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 0x0001:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc & 0xFFFF

class PureSolarmanV5Client:
    # Reine Python-Implementierung des Solarman V5 Protokolls (Port 8899).
    # Benötigt KEINE externen Abhängigkeiten (kein multiprocessing/sem_open).
    START = 0xA5
    END = 0x15
    CONTROL_REQUEST = 0x4510

    def __init__(self, host: str, serial: int, port: int = 8899, slave_id: int = 1, timeout: float = 5.0):
        self.host = host
        self.serial = serial
        self.port = port
        self.slave_id = slave_id
        self.timeout = timeout
        self.seq = 1
        self.sock = None

    def _get_socket(self):
        if self.sock is not None:
            return self.sock
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(self.timeout)
        s.connect((self.host, self.port))
        self.sock = s
        return self.sock

    def close(self):
        if self.sock:
            try:
                self.sock.shutdown(socket.SHUT_RDWR)
            except Exception:
                pass
            try:
                self.sock.close()
            except Exception:
                pass
            self.sock = None

    def read_holding_registers(self, start_reg: int, count: int) -> list:
        # Modbus RTU PDU = Function (0x03) + Start Reg (2 bytes, big-endian) + Quantity (2 bytes, big-endian)
        modbus_pdu = struct.pack(">B H H", 0x03, start_reg, count)
        rtu_frame = bytes([self.slave_id]) + modbus_pdu
        crc = crc16_modbus(rtu_frame)
        rtu_frame += struct.pack("<H", crc)

        # Solarman V5 Payload: Type(0x02) + Sensor(0x0000) + Delivery(0) + PowerOn(0) + Offset(0)
        v5_payload = struct.pack("<B H I I I", 0x02, 0x0000, 0, 0, 0) + rtu_frame
        payload_len = len(v5_payload)

        # Solarman V5 Header: Start(0xA5) + Len(2, <H) + Control(2, <H) + Seq(2, <H) + Serial(4, <I)
        header = struct.pack("<B H H H I", self.START, payload_len, self.CONTROL_REQUEST, self.seq & 0xFFFF, self.serial)
        self.seq = (self.seq + 1) & 0xFFFF

        packet_no_trailer = header + v5_payload
        checksum = sum(packet_no_trailer[1:]) & 0xFF
        trailer = struct.pack("<B B", checksum, self.END)
        full_packet = packet_no_trailer + trailer

        try:
            sock = self._get_socket()
            sock.sendall(full_packet)

            # Empfange Header (mindestens 11 Bytes)
            resp = b""
            while len(resp) < 11:
                chunk = sock.recv(1024)
                if not chunk:
                    raise ConnectionError("Keine Daten vom Deye Stick empfangen (Socket getrennt)")
                resp += chunk

            if resp[0] != self.START:
                raise ValueError(f"Ungültiges Start-Byte in Solarman Antwort: {hex(resp[0])}")

            payload_len = struct.unpack("<H", resp[1:3])[0]
            total_expected = 11 + payload_len + 2
            while len(resp) < total_expected:
                chunk = sock.recv(1024)
                if not chunk:
                    break
                resp += chunk

            if len(resp) < total_expected or resp[-1] != self.END:
                raise ValueError(f"Unvollständige Antwort empfangen ({len(resp)}/{total_expected} Bytes)")

            payload = resp[11:11 + payload_len]
            if len(payload) < 17:
                raise ValueError(f"Antwort-Payload zu kurz ({len(payload)} Bytes)")

            modbus_part = payload[14:]
            if len(modbus_part) < 3:
                raise ValueError("Modbus RTU Anteil fehlt in Antwort")

            func = modbus_part[1]
            if func & 0x80:
                err_code = modbus_part[2] if len(modbus_part) > 2 else 0
                raise RuntimeError(f"Deye Modbus Fehlercode {hex(func)} (Exception Code {err_code})")

            byte_count = modbus_part[2]
            raw_regs = modbus_part[3:3 + byte_count]
            regs = []
            for i in range(0, len(raw_regs), 2):
                if i + 1 < len(raw_regs):
                    regs.append(struct.unpack(">H", raw_regs[i:i+2])[0])
            return regs
        except Exception as e:
            self.close()
            raise e

class PureModbusTCPClient:
    def __init__(self, host: str, port: int = 502, slave_id: int = 1, timeout: float = 5.0):
        self.host = host
        self.port = port
        self.slave_id = slave_id
        self.timeout = timeout
        self.tid = 1
        self.sock = None

    def _get_socket(self):
        if self.sock is not None:
            return self.sock
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(self.timeout)
        s.connect((self.host, self.port))
        self.sock = s
        return self.sock

    def close(self):
        if self.sock:
            try:
                self.sock.shutdown(socket.SHUT_RDWR)
            except Exception:
                pass
            try:
                self.sock.close()
            except Exception:
                pass
            self.sock = None

    def read_holding_registers(self, start_reg: int, count: int) -> list:
        try:
            sock = self._get_socket()
            pdu = struct.pack(">B H H", 0x03, start_reg, count)
            mbap = struct.pack(">H H H B", self.tid, 0x0000, len(pdu) + 1, self.slave_id)
            self.tid = (self.tid + 1) & 0xFFFF
            sock.sendall(mbap + pdu)

            resp = b""
            while len(resp) < 9:
                chunk = sock.recv(1024)
                if not chunk:
                    raise ConnectionError("Modbus TCP Socket geschlossen")
                resp += chunk

            func = resp[7]
            if func & 0x80:
                raise RuntimeError(f"Modbus TCP Fehler (Funktion {hex(func)})")

            byte_count = resp[8]
            while len(resp) < 9 + byte_count:
                chunk = sock.recv(1024)
                if not chunk:
                    break
                resp += chunk

            data = resp[9:9 + byte_count]
            regs = []
            for i in range(0, len(data), 2):
                if i + 1 < len(data):
                    regs.append(struct.unpack(">H", data[i:i+2])[0])
            return regs
        except Exception as e:
            self.close()
            raise e

def auto_discover_serial(host: str, username: str = "admin", password: str = "admin") -> int:
    url = f"http://{host}/status.html"
    req = urllib.request.Request(url)
    auth = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("utf-8")
    req.add_header("Authorization", f"Basic {auth}")
    
    try:
        with urllib.request.urlopen(req, timeout=2) as resp:
            html = resp.read().decode("utf-8", errors="ignore")
            candidates = re.findall(r'\b([0-9]{10})\b', html)
            for c in candidates:
                sn = int(c)
                if 1000000000 <= sn <= 4294967295:
                    return sn
    except Exception:
        pass
    return 0

def to_signed16(val: int) -> int:
    return val if val < 0x8000 else val - 0x10000

class DeyeModbusClient:
    def __init__(self, host: str, port: int = 8899, slave_id: int = 1, serial_number: int = 0, timeout: float = 5.0):
        self.host = host
        self.port = port
        self.slave_id = slave_id
        self.serial_number = serial_number
        self.timeout = timeout
        self.client = None
        self.INTER_BLOCK_DELAY = 0.08  # 80ms Pause zwischen Registerblöcken
        self.RECONNECT_DELAY = 0.5    # 500ms Pause vor Neuaufbau nach Abbruch

    def _init_client(self):
        if self.port == 502:
            self.client = PureModbusTCPClient(
                host=self.host,
                port=self.port,
                slave_id=self.slave_id,
                timeout=self.timeout
            )
        else:
            sn = self.serial_number
            if not sn or sn == 0:
                sn = auto_discover_serial(self.host)
                if not sn or sn == 0:
                    sn = 1109501211
                self.serial_number = sn

            self.client = PureSolarmanV5Client(
                host=self.host,
                serial=sn,
                port=self.port,
                slave_id=self.slave_id,
                timeout=self.timeout
            )

    def close(self):
        if self.client:
            self.client.close()
            self.client = None

    def read_holding_registers_safe(self, start_reg: int, count: int, max_retries: int = 2) -> list:
        last_exc = None
        for attempt in range(max_retries + 1):
            try:
                if not self.client:
                    self._init_client()
                regs = self.client.read_holding_registers(start_reg, count)
                if not regs or len(regs) < count:
                    raise ValueError(f"Unvollständiger Block ab Reg {start_reg}: {len(regs) if regs else 0}/{count} Register")
                return regs
            except Exception as e:
                last_exc = e
                self.close()
                if attempt < max_retries:
                    time.sleep(self.RECONNECT_DELAY)
        raise last_exc

    def read_deye_12k_data(self) -> dict:
        data = {}

        # Block 1: Status & Tageserträge (500-541, 42 Regs)
        b1 = self.read_holding_registers_safe(500, 42)
        time.sleep(self.INTER_BLOCK_DELAY)

        data["status_code"] = b1[0]
        status_map = {0: "Standby", 1: "Self-Test", 2: "Normal (Netzverbunden)", 3: "Alarm", 4: "Störung"}
        data["status_text"] = status_map.get(b1[0], f"Code {b1[0]}")
        data["energy_grid_buy_today_kwh"] = round(b1[20] * 0.1, 2)
        data["energy_grid_sell_today_kwh"] = round(b1[21] * 0.1, 2)
        data["energy_bat_charge_today_kwh"] = round(b1[22] * 0.1, 2)
        data["energy_bat_dischg_today_kwh"] = round(b1[23] * 0.1, 2)
        data["energy_load_today_kwh"] = round(b1[26] * 0.1, 2)
        data["energy_pv_today_kwh"] = round(b1[29] * 0.1, 2)
        data["temp_dc_celsius"] = round((b1[40] - 1000) * 0.1, 1) if b1[40] >= 1000 else round(b1[40] * 0.1 - 100, 1)
        data["temp_ac_celsius"] = round((b1[41] - 1000) * 0.1, 1) if b1[41] >= 1000 else round(b1[41] * 0.1 - 100, 1)

        # Block 2: Batterie & Netz (586-612, 27 Regs)
        b2 = self.read_holding_registers_safe(586, 27)
        time.sleep(self.INTER_BLOCK_DELAY)

        raw_bat_temp = b2[0]
        data["temp_battery_celsius"] = round((raw_bat_temp - 1000) * 0.1, 1) if raw_bat_temp >= 1000 else round(raw_bat_temp * 0.1 - 100, 1)
        data["battery_voltage_v"] = round(b2[1] * 0.01, 2)
        data["battery_soc_percent"] = b2[2]

        bat_pow_reg = to_signed16(b2[3])
        bat_curr = round(to_signed16(b2[4]) * 0.02, 2)
        data["battery_current_a"] = bat_curr

        # Plausibilitäts-Check: Wenn Register 590 Null meldet, aber Strom fließt (Laden/Entladen)
        if bat_pow_reg == 0 and abs(bat_curr) > 0.5:
            data["battery_power_w"] = int(round(data["battery_voltage_v"] * bat_curr))
        else:
            data["battery_power_w"] = bat_pow_reg

        data["grid_voltage_l1_v"] = round(b2[12] * 0.1, 1)
        data["grid_voltage_l2_v"] = round(b2[13] * 0.1, 1)
        data["grid_voltage_l3_v"] = round(b2[14] * 0.1, 1)
        data["grid_power_l1_w"] = to_signed16(b2[18])
        data["grid_power_l2_w"] = to_signed16(b2[19])
        data["grid_power_l3_w"] = to_signed16(b2[20])
        data["grid_power_total_w"] = to_signed16(b2[21])
        
        freq_raw = b2[22] if b2[22] > 0 else (b2[23] if len(b2) > 23 else 5000)
        if freq_raw > 10000:
            freq_raw = 5000 + to_signed16(freq_raw)
        data["grid_frequency_hz"] = round(freq_raw * (0.01 if freq_raw > 1000 else 0.1), 2)

        # Block 3: Phasenleistung & Last (625-653, 29 Regs)
        b3 = self.read_holding_registers_safe(625, 29)
        time.sleep(self.INTER_BLOCK_DELAY)

        data["inverter_power_l1_w"] = to_signed16(b3[0])
        data["inverter_power_l2_w"] = to_signed16(b3[1])
        data["inverter_power_l3_w"] = to_signed16(b3[2])
        data["inverter_power_total_w"] = to_signed16(b3[11])
        data["load_power_l1_w"] = to_signed16(b3[25])
        data["load_power_l2_w"] = to_signed16(b3[26])
        data["load_power_l3_w"] = to_signed16(b3[27])
        data["load_power_total_w"] = to_signed16(b3[28])

        # Block 4: PV Strings (672-679, 8 Regs)
        b4 = self.read_holding_registers_safe(672, 8)

        data["pv1_voltage_v"] = round(b4[4] * 0.1, 1)
        data["pv1_current_a"] = round(b4[5] * 0.1, 1)
        data["pv1_power_w"] = round(data["pv1_voltage_v"] * data["pv1_current_a"], 1)
        data["pv2_voltage_v"] = round(b4[6] * 0.1, 1)
        data["pv2_current_a"] = round(b4[7] * 0.1, 1)
        data["pv2_power_w"] = round(data["pv2_voltage_v"] * data["pv2_current_a"], 1)
        data["pv_power_total_w"] = round(data["pv1_power_w"] + data["pv2_power_w"], 1)

        data["logger_serial"] = self.serial_number
        data["timestamp"] = time.strftime("%Y-%m-%d %H:%M:%S")
        data["online"] = True
        return data

latest_telemetry = {}
telemetry_lock = threading.Lock()

def render_terminal_dashboard(data: dict):
    C_RESET = "\033[0m"
    C_BOLD = "\033[1m"
    C_GREEN = "\033[32m"
    C_YELLOW = "\033[33m"
    C_BLUE = "\033[34m"
    C_CYAN = "\033[36m"
    C_RED = "\033[31m"

    print("\033[H\033[J", end="")
    print(f"{C_BOLD}{C_CYAN}========================================================================{C_RESET}")
    print(f"{C_BOLD}{C_YELLOW}        DEYE SUN-12K-SG04LP3 MODBUS TELEMETRIE LOG{C_RESET}")
    print(f"        Zeit: {data.get('timestamp', 'N/A')} | Logger SN: {data.get('logger_serial', 'N/A')}")
    print(f"{C_BOLD}{C_CYAN}========================================================================{C_RESET}")

    st = data.get("status_text", "N/A")
    st_color = C_GREEN if "Normal" in st else C_YELLOW
    print(f"{C_BOLD}Status:{C_RESET} {st_color}{st}{C_RESET}")
    print("------------------------------------------------------------------------")

    pv_total = data.get("pv_power_total_w", 0.0)
    print(f"{C_BOLD}{C_GREEN}☀️  SOLAR / PV (Gesamt: {pv_total:.1f} W | Heute: {data.get('energy_pv_today_kwh', 0)} kWh){C_RESET}")
    print(f"   String 1: {data.get('pv1_power_w', 0):>6.1f} W  ({data.get('pv1_voltage_v', 0):>5.1f} V, {data.get('pv1_current_a', 0):>4.1f} A)")
    print(f"   String 2: {data.get('pv2_power_w', 0):>6.1f} W  ({data.get('pv2_voltage_v', 0):>5.1f} V, {data.get('pv2_current_a', 0):>4.1f} A)")
    print("------------------------------------------------------------------------")

    bat_pow = data.get("battery_power_w", 0)
    bat_soc = data.get("battery_soc_percent", 0)
    bat_status = "Laden" if bat_pow > 0 else ("Entladen" if bat_pow < 0 else "Standby")
    bat_color = C_GREEN if bat_pow >= 0 else C_YELLOW
    print(f"{C_BOLD}{C_CYAN}🔋 BATTERIE ({bat_soc}% | {bat_color}{bat_status} {abs(bat_pow)} W{C_RESET}){C_RESET}")
    print(f"   Spannung: {data.get('battery_voltage_v', 0):>5.2f} V | Strom: {data.get('battery_current_a', 0):>6.2f} A | Temp: {data.get('temp_battery_celsius', 0)} °C")
    print("------------------------------------------------------------------------")

    grid_pow = data.get("grid_power_total_w", 0)
    grid_status = f"Bezug ({grid_pow} W)" if grid_pow >= 0 else f"Einspeisung ({abs(grid_pow)} W)"
    grid_color = C_RED if grid_pow > 0 else C_GREEN
    print(f"{C_BOLD}{C_BLUE}🔌 NETZ (Grid: {grid_color}{grid_status}{C_RESET} | {data.get('grid_frequency_hz', 0)} Hz){C_RESET}")
    print(f"   L1: {data.get('grid_voltage_l1_v', 0):>5.1f} V | {data.get('grid_power_l1_w', 0):>6} W  ||  L2: {data.get('grid_voltage_l2_v', 0):>5.1f} V | {data.get('grid_power_l2_w', 0):>6} W  ||  L3: {data.get('grid_voltage_l3_v', 0):>5.1f} V | {data.get('grid_power_l3_w', 0):>6} W")
    print("------------------------------------------------------------------------")

    load_pow = data.get("load_power_total_w", 0)
    print(f"{C_BOLD}{C_YELLOW}🏠 HAUSVERBRAUCH (Gesamt: {load_pow} W | Heute: {data.get('energy_load_today_kwh', 0)} kWh){C_RESET}")
    print(f"   L1: {data.get('load_power_l1_w', 0):>6} W | L2: {data.get('load_power_l2_w', 0):>6} W | L3: {data.get('load_power_l3_w', 0):>6} W")
    print("------------------------------------------------------------------------")
    print(f"🌡️ Inverter Temp: DC {data.get('temp_dc_celsius', 0)} °C | AC {data.get('temp_ac_celsius', 0)} °C")
    print(f"{C_BOLD}{C_CYAN}========================================================================{C_RESET}")

HTML_TEMPLATE = $TQ<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Deye 12K Solar Dashboard</title>
    <style>
        :root {
            --bg-color: #0b0f19;
            --card-bg: #151e2e;
            --card-inner: #1c273c;
            --text-main: #f8fafc;
            --text-sub: #94a3b8;
            --accent-solar: #f59e0b;
            --accent-bat: #10b981;
            --accent-grid: #3b82f6;
            --accent-load: #ec4899;
            --border-color: rgba(255, 255, 255, 0.08);
            --danger: #ef4444;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            padding: 14px;
            min-height: 100vh;
        }
        .container { max-width: 1200px; margin: 0 auto; }
        header {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 12px;
            margin-bottom: 18px;
            gap: 10px;
        }
        .header-title h1 {
            font-size: 1.35rem;
            font-weight: 700;
            color: #fbbf24;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .header-title .sub {
            font-size: 0.8rem;
            color: var(--text-sub);
            margin-top: 2px;
        }
        .badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #1e293b;
            padding: 6px 12px;
            border-radius: 9999px;
            font-size: 0.8rem;
            font-weight: 500;
            color: var(--text-sub);
            border: 1px solid var(--border-color);
        }
        .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #10b981;
            box-shadow: 0 0 8px #10b981;
            display: inline-block;
        }
        .dot.pulse { animation: pulse-dot 1.8s infinite ease-in-out; }
        @keyframes pulse-dot { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.85); } }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 16px;
        }
        .card {
            background: var(--card-bg);
            border-radius: 14px;
            padding: 16px;
            border: 1px solid var(--border-color);
            box-shadow: 0 8px 20px -6px rgba(0, 0, 0, 0.4);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            overflow: hidden;
        }
        .card::before {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0; height: 3px;
        }
        .card-solar::before { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
        .card-bat::before { background: linear-gradient(90deg, #10b981, #34d399); }
        .card-grid::before { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
        .card-load::before { background: linear-gradient(90deg, #ec4899, #f472b6); }

        .card-header {
            font-size: 1.05rem;
            font-weight: 700;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
        }
        .metric-big {
            font-size: 2.2rem;
            font-weight: 800;
            margin: 6px 0 12px 0;
            letter-spacing: -0.5px;
            line-height: 1.1;
        }
        .solar-color { color: var(--accent-solar); }
        .bat-color { color: var(--accent-bat); }
        .grid-color { color: var(--accent-grid); }
        .load-color { color: var(--accent-load); }

        .sub-metrics {
            margin-top: auto;
            font-size: 0.85rem;
            background: var(--card-inner);
            padding: 10px 12px;
            border-radius: 10px;
            border: 1px solid var(--border-color);
        }
        .row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 5px 0;
            border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }
        .row:last-child { border-bottom: none; }
        .row span:first-child { color: var(--text-sub); }
        .val { color: var(--text-main); font-weight: 600; }
        .val-badge {
            font-size: 0.75rem;
            padding: 2px 8px;
            border-radius: 6px;
            background: rgba(255, 255, 255, 0.1);
        }

        .flow-summary {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 14px;
            padding: 12px 16px;
            margin-bottom: 16px;
            display: flex;
            flex-wrap: wrap;
            justify-content: space-around;
            align-items: center;
            gap: 12px;
            text-align: center;
        }
        .flow-item { flex: 1 1 120px; }
        .flow-label { font-size: 0.75rem; color: var(--text-sub); text-transform: uppercase; letter-spacing: 0.5px; }
        .flow-val { font-size: 1.15rem; font-weight: 700; margin-top: 2px; }

        footer {
            margin-top: 20px;
            text-align: center;
            font-size: 0.75rem;
            color: var(--text-sub);
            padding-top: 10px;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <div class="header-title">
                <h1>⚡ Deye SUN-12K Hybrid Inverter</h1>
                <div class="sub">Modbus TCP (Solarman V5) &bull; Live Telemetrie</div>
            </div>
            <div class="header-status">
                <div class="badge" id="status-badge">
                    <span class="dot pulse" id="status-dot"></span>
                    <span id="last-update">Verbinde...</span>
                </div>
            </div>
        </header>

        <div class="flow-summary">
            <div class="flow-item">
                <div class="flow-label">☀️ Erzeugung</div>
                <div class="flow-val solar-color" id="sum-solar">0 W</div>
            </div>
            <div class="flow-item">
                <div class="flow-label">🔋 Speicher</div>
                <div class="flow-val bat-color" id="sum-bat">0 %</div>
            </div>
            <div class="flow-item">
                <div class="flow-label">🔌 Netz</div>
                <div class="flow-val grid-color" id="sum-grid">0 W</div>
            </div>
            <div class="flow-item">
                <div class="flow-label">🏠 Verbrauch</div>
                <div class="flow-val load-color" id="sum-load">0 W</div>
            </div>
        </div>

        <div class="grid">
            <div class="card card-solar">
                <div>
                    <div class="card-header solar-color">
                        <span>☀️ Solar PV</span>
                        <span class="val-badge" id="pv-today-badge">Heute: 0 kWh</span>
                    </div>
                    <div class="metric-big solar-color" id="pv-total">0 W</div>
                </div>
                <div class="sub-metrics">
                    <div class="row"><span>PV String 1:</span><span class="val" id="pv1">0 W</span></div>
                    <div class="row"><span>PV String 2:</span><span class="val" id="pv2">0 W</span></div>
                    <div class="row"><span>Tagesertrag:</span><span class="val" id="pv-today">0 kWh</span></div>
                </div>
            </div>

            <div class="card card-bat">
                <div>
                    <div class="card-header bat-color">
                        <span>🔋 Batterie</span>
                        <span class="val-badge" id="bat-status-badge">Standby</span>
                    </div>
                    <div class="metric-big bat-color" id="bat-soc">0 %</div>
                </div>
                <div class="sub-metrics">
                    <div class="row"><span>Leistung:</span><span class="val" id="bat-power">0 W</span></div>
                    <div class="row"><span>Spannung / Strom:</span><span class="val" id="bat-vi">0.0 V / 0.0 A</span></div>
                    <div class="row"><span>Batterietemperatur:</span><span class="val" id="bat-temp">0 °C</span></div>
                    <div class="row"><span>Ladung / Entladung:</span><span class="val" id="bat-today">0 / 0 kWh</span></div>
                </div>
            </div>

            <div class="card card-grid">
                <div>
                    <div class="card-header grid-color">
                        <span>🔌 Netzanschluss</span>
                        <span class="val-badge" id="grid-freq">50.0 Hz</span>
                    </div>
                    <div class="metric-big grid-color" id="grid-total">0 W</div>
                </div>
                <div class="sub-metrics">
                    <div class="row"><span>Phase L1:</span><span class="val" id="grid-l1">0 V / 0 W</span></div>
                    <div class="row"><span>Phase L2:</span><span class="val" id="grid-l2">0 V / 0 W</span></div>
                    <div class="row"><span>Phase L3:</span><span class="val" id="grid-l3">0 V / 0 W</span></div>
                    <div class="row"><span>Kauf / Verkauf:</span><span class="val" id="grid-today">0 / 0 kWh</span></div>
                </div>
            </div>

            <div class="card card-load">
                <div>
                    <div class="card-header load-color">
                        <span>🏠 Hausverbrauch</span>
                        <span class="val-badge" id="load-today-badge">Heute: 0 kWh</span>
                    </div>
                    <div class="metric-big load-color" id="load-total">0 W</div>
                </div>
                <div class="sub-metrics">
                    <div class="row"><span>Phasen L1/L2/L3:</span><span class="val" id="load-phases">0 / 0 / 0 W</span></div>
                    <div class="row"><span>Tagesverbrauch:</span><span class="val" id="load-today">0 kWh</span></div>
                    <div class="row"><span>Inverter Temp:</span><span class="val" id="inv-temps">0 / 0 °C</span></div>
                </div>
            </div>
        </div>

        <footer>
            <div id="footer-details">Deye SUN-12K &bull; Modbus Solarman V5</div>
        </footer>
    </div>

    <script>
        function fmtW(w) {
            if (w === undefined || w === null || isNaN(w)) return "0 W";
            const abs = Math.abs(w);
            if (abs >= 1000) return (w / 1000).toFixed(2) + " kW";
            return Math.round(w) + " W";
        }

        async function fetchMetrics() {
            try {
                let d = null;
                try {
                    const res = await fetch('/api/data', { cache: 'no-store' });
                    if (res.ok) d = await res.json();
                } catch(e) {}

                if (!d || Object.keys(d).length === 0) return;

                const dot = document.getElementById('status-dot');
                dot.className = 'dot pulse';
                const st = d.status_text || 'Normal';
                const timeStr = d.timestamp ? d.timestamp.split(' ')[1] : '';
                document.getElementById('last-update').innerText = timeStr + ' (' + st + ')';

                const pvTot = d.pv_power_total_w || 0;
                const batPow = d.battery_power_w || 0;
                const gridPow = d.grid_power_total_w || 0;
                const loadTot = d.load_power_total_w || 0;

                document.getElementById('sum-solar').innerText = fmtW(pvTot);
                document.getElementById('sum-bat').innerText = (d.battery_soc_percent || 0) + ' %';
                document.getElementById('sum-grid').innerText = (gridPow >= 0 ? '+' : '') + fmtW(gridPow);
                document.getElementById('sum-load').innerText = fmtW(loadTot);

                document.getElementById('pv-total').innerText = fmtW(pvTot);
                document.getElementById('pv1').innerText = (d.pv1_power_w || 0) + ' W (' + (d.pv1_voltage_v || 0) + ' V, ' + (d.pv1_current_a || 0) + ' A)';
                document.getElementById('pv2').innerText = (d.pv2_power_w || 0) + ' W (' + (d.pv2_voltage_v || 0) + ' V, ' + (d.pv2_current_a || 0) + ' A)';
                document.getElementById('pv-today').innerText = (d.energy_pv_today_kwh || 0) + ' kWh';
                document.getElementById('pv-today-badge').innerText = 'Heute: ' + (d.energy_pv_today_kwh || 0) + ' kWh';

                const soc = d.battery_soc_percent || 0;
                document.getElementById('bat-soc').innerText = soc + ' %';
                let batStatusText = 'Standby';
                if (batPow > 20) batStatusText = 'Laden (' + fmtW(batPow) + ')';
                else if (batPow < -20) batStatusText = 'Entladen (' + fmtW(Math.abs(batPow)) + ')';
                document.getElementById('bat-status-badge').innerText = batStatusText;
                document.getElementById('bat-power').innerText = (batPow >= 0 ? '+' : '') + fmtW(batPow);
                document.getElementById('bat-vi').innerText = (d.battery_voltage_v || 0).toFixed(1) + ' V / ' + (d.battery_current_a || 0).toFixed(1) + ' A';
                document.getElementById('bat-temp').innerText = (d.temp_battery_celsius || 0) + ' °C';
                document.getElementById('bat-today').innerText = (d.energy_bat_charge_today_kwh || 0) + ' / ' + (d.energy_bat_dischg_today_kwh || 0) + ' kWh';

                document.getElementById('grid-total').innerText = (gridPow >= 0 ? '+' : '') + fmtW(gridPow);
                document.getElementById('grid-freq').innerText = (d.grid_frequency_hz || 50.0) + ' Hz';
                document.getElementById('grid-l1').innerText = (d.grid_voltage_l1_v || 0) + ' V / ' + (d.grid_power_l1_w || 0) + ' W';
                document.getElementById('grid-l2').innerText = (d.grid_voltage_l2_v || 0) + ' V / ' + (d.grid_power_l2_w || 0) + ' W';
                document.getElementById('grid-l3').innerText = (d.grid_voltage_l3_v || 0) + ' V / ' + (d.grid_power_l3_w || 0) + ' W';
                document.getElementById('grid-today').innerText = 'K: ' + (d.energy_grid_buy_today_kwh || 0) + ' / V: ' + (d.energy_grid_sell_today_kwh || 0) + ' kWh';

                document.getElementById('load-total').innerText = fmtW(loadTot);
                document.getElementById('load-phases').innerText = (d.load_power_l1_w || 0) + ' / ' + (d.load_power_l2_w || 0) + ' / ' + (d.load_power_l3_w || 0) + ' W';
                document.getElementById('load-today').innerText = (d.energy_load_today_kwh || 0) + ' kWh';
                document.getElementById('load-today-badge').innerText = 'Heute: ' + (d.energy_load_today_kwh || 0) + ' kWh';
                document.getElementById('inv-temps').innerText = (d.temp_dc_celsius || 0) + ' °C / ' + (d.temp_ac_celsius || 0) + ' °C';

                if (d.logger_serial) {
                    document.getElementById('footer-details').innerText = 'Deye SUN-12K &bull; Logger SN: ' + d.logger_serial + ' &bull; ' + (d.timestamp || '');
                }
            } catch(e) {}
        }

        fetchMetrics();
        setInterval(fetchMetrics, 2000);
    </script>
</body>
</html>$TQ

class WebDashboardServer(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_HEAD(self):
        self._handle_response(include_body=False)

    def do_GET(self):
        self._handle_response(include_body=True)

    def _handle_response(self, include_body: bool = True):
        parsed = self.path.split("?")[0]
        try:
            if parsed == "/api/data":
                with telemetry_lock:
                    payload = json.dumps(latest_telemetry).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(payload)))
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
                self.send_header("Connection", "close")
                self.end_headers()
                if include_body:
                    self.wfile.write(payload)
            elif parsed == "/favicon.ico":
                self.send_response(204)
                self.send_header("Content-Length", "0")
                self.send_header("Connection", "close")
                self.end_headers()
            else:
                payload = HTML_TEMPLATE.encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(payload)))
                self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
                self.send_header("Connection", "close")
                self.end_headers()
                if include_body:
                    self.wfile.write(payload)
        except Exception:
            pass

    def log_message(self, format, *args):
        pass

class ThreadedHTTPServer(socketserver.ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

active_server = None

def run_web_server(port: int):
    global active_server
    for p in range(port, port + 10):
        try:
            server = ThreadedHTTPServer(("0.0.0.0", p), WebDashboardServer)
            active_server = server
            if p != port:
                print(f"ℹ️ Web Dashboard läuft auf Ausweich-Port {p}")
            server.serve_forever()
            break
        except OSError as e:
            if e.errno == 98 or "address already in use" in str(e).lower():
                continue
            break
        except Exception:
            break

def main():
    parser = argparse.ArgumentParser(description="Read Deye 12K Hybrid Inverter via Modbus TCP / Solarman V5")
    parser.add_argument("--host", type=str, default=INVERTER_IP, help="Deye Inverter IP")
    parser.add_argument("--port", type=int, default=INVERTER_PORT, help="Modbus Port")
    parser.add_argument("--slave-id", type=int, default=SLAVE_ID, help="Slave ID")
    parser.add_argument("--serial", type=int, default=LOGGER_SERIAL, help="Logger SN")
    parser.add_argument("--watch", type=int, default=POLL_INTERVAL, help="Polling Intervall in Sekunden")
    parser.add_argument("--web", type=int, default=WEB_PORT, help="Web Dashboard Port")
    parser.add_argument("--no-web", action="store_true", help="Deaktivieren")

    args, unknown = parser.parse_known_args()

    print(f"\u001B[1m\u001B[36m⚡ Deye SUN-12K Modbus Reader wird gestartet...\u001B[0m")
    print(f"📌 Ziel: {args.host}:{args.port} (Slave ID: {args.slave_id})")
    if args.serial > 0:
        print(f"🔑 Logger Seriennummer: {args.serial}")
    else:
        print(f"🔍 Logger Seriennummer: Auto-Discovery")

    client = DeyeModbusClient(
        host=args.host,
        port=args.port,
        slave_id=args.slave_id,
        serial_number=args.serial,
        timeout=5.0
    )

    global latest_telemetry

    if not args.no_web:
        web_port = args.web
        t = threading.Thread(target=run_web_server, args=(web_port,), daemon=True)
        t.start()
        print(f"🌐 Lokales Web-Dashboard bereit auf Port {web_port}")

    try:
        while True:
            try:
                data = client.read_deye_12k_data()
                with telemetry_lock:
                    latest_telemetry = data

                render_terminal_dashboard(data)

            except Exception as e:
                print(f"\u001B[31m[Modbus Fehler]\u001B[0m {e}", file=sys.stderr)
            
            time.sleep(args.watch)

    except KeyboardInterrupt:
        client.close()
        print("\nBeendet.")

if __name__ == "__main__":
    main()
"""

    val MATPLOTLIB_SCRIPT = """# Matplotlib Plot Visualization Demo
import matplotlib.pyplot as plt
import numpy as np

# Create data
x = np.linspace(0, 4 * np.pi, 200)
y1 = np.sin(x)
y2 = np.cos(x) * np.exp(-x / 8)

# Create figure with dark styling
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 6), facecolor='#0D1117')

# Plot 1: Sine wave
ax1.set_facecolor('#161B22')
ax1.plot(x, y1, color='#58A6FF', linewidth=2.5, label='sin(x)')
ax1.fill_between(x, y1, color='#58A6FF', alpha=0.2)
ax1.set_title('Trigonometric Waveforms', color='#F0F6FC', fontsize=14, pad=10)
ax1.tick_params(colors='#8B949E')
ax1.grid(True, linestyle='--', alpha=0.3, color='#8B949E')
ax1.legend(loc='upper right', facecolor='#21262D', edgecolor='#30363D', labelcolor='#F0F6FC')

# Plot 2: Damped Cosine
ax2.set_facecolor('#161B22')
ax2.plot(x, y2, color='#3FB950', linewidth=2.5, linestyle='-', label='cos(x) * exp(-x/8)')
ax2.plot(x, -y2, color='#F85149', linewidth=1.5, linestyle=':', label='Envelope')
ax2.set_xlabel('Time (t)', color='#8B949E', fontsize=12)
ax2.tick_params(colors='#8B949E')
ax2.grid(True, linestyle='--', alpha=0.3, color='#8B949E')
ax2.legend(loc='upper right', facecolor='#21262D', edgecolor='#30363D', labelcolor='#F0F6FC')

plt.tight_layout()
print("Rendering plot to Visual Output tab...")
plt.show()
print("Matplotlib figure displayed successfully!")
"""

    val NUMPY_SCRIPT = """# NumPy Matrix Algebra & Statistics
import numpy as np

print("=== NumPy Native Engine Demonstration ===")

# Create random matrices
np.random.seed(42)
matrix_a = np.random.randint(1, 10, size=(4, 4))
matrix_b = np.random.randint(1, 10, size=(4, 4))

print("Matrix A (4x4):")
print(matrix_a)
print("\\nMatrix B (4x4):")
print(matrix_b)

# Matrix Multiplication
product = np.matmul(matrix_a, matrix_b)
print("\\nMatrix Multiplication (A @ B):")
print(product)

# Determinant & Inversion
det_a = np.linalg.det(matrix_a)
print(f"\\nDeterminant of A: {det_a:.4f}")

if det_a != 0:
    inv_a = np.linalg.inv(matrix_a)
    print("Inverse of A:")
    print(np.round(inv_a, 3))

# Statistical aggregates
arr = np.linspace(10, 100, 10)
print(f"\\nSample Array: {arr}")
print(f"Mean: {np.mean(arr):.2f}, Std Dev: {np.std(arr):.2f}")
print("NumPy calculations finished with full native acceleration!")
"""

    val HTML_DASHBOARD_SCRIPT = """# Interactive HTML / SVG Visual Output
html_code = '''<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, sans-serif; }
        body { background: #0d1117; color: #c9d1d9; padding: 16px; }
        .card { background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
        .title { color: #58a6ff; font-size: 20px; font-weight: bold; margin-bottom: 8px; }
        .stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 16px; }
        .stat-box { background: #21262d; padding: 12px; border-radius: 8px; border-left: 4px solid #238636; }
        .stat-val { font-size: 22px; font-weight: bold; color: #f0f6fc; }
        .stat-lbl { font-size: 12px; color: #8b949e; }
        svg { width: 100%; height: 160px; margin-top: 12px; }
        .bar { fill: #1f6feb; rx: 4px; transition: fill 0.3s; }
        .bar:hover { fill: #58a6ff; }
        .btn { background: #238636; color: white; border: none; padding: 10px 18px; border-radius: 6px; font-weight: bold; margin-top: 12px; cursor: pointer; }
    </style>
</head>
<body>
    <div class="card">
        <div class="title">⚡ Python Visual Dashboard</div>
        <p style="color: #8b949e; font-size: 14px;">Live rendered directly inside Android WebView</p>
        
        <div class="stat-grid">
            <div class="stat-box">
                <div class="stat-val">99.8%</div>
                <div class="stat-lbl">Engine Stability</div>
            </div>
            <div class="stat-box" style="border-left-color: #58a6ff;">
                <div class="stat-val">0.42 ms</div>
                <div class="stat-lbl">Coroutines Latency</div>
            </div>
        </div>

        <svg viewBox="0 0 300 120">
            <rect class="bar" x="20" y="40" width="35" height="80" />
            <rect class="bar" x="75" y="20" width="35" height="100" />
            <rect class="bar" x="130" y="60" width="35" height="60" />
            <rect class="bar" x="185" y="10" width="35" height="110" />
            <rect class="bar" x="240" y="30" width="35" height="90" fill="#3fb950" />
            <text x="37" y="115" fill="#8b949e" font-size="10" text-anchor="middle">Q1</text>
            <text x="92" y="115" fill="#8b949e" font-size="10" text-anchor="middle">Q2</text>
            <text x="147" y="115" fill="#8b949e" font-size="10" text-anchor="middle">Q3</text>
            <text x="202" y="115" fill="#8b949e" font-size="10" text-anchor="middle">Q4</text>
            <text x="257" y="115" fill="#8b949e" font-size="10" text-anchor="middle">Q5</text>
        </svg>

        <button class="btn" onclick="alert('Interactive JavaScript works inside Python Runner!')">Click for Interactive JS</button>
    </div>
</body>
</html>'''

with open("dashboard_report.html", "w", encoding="utf-8") as f:
    f.write(html_code)

print("Generated interactive HTML report:")
print(html_code)
print("\\nCheck the 'Visual Output' tab to interact with the dashboard!")
"""

    val SAMPLES = listOf(
        "deye_12k_inverter.py" to DEYE_SCRIPT,
        "1_matplotlib_waves.py" to MATPLOTLIB_SCRIPT,
        "2_numpy_matrix.py" to NUMPY_SCRIPT,
        "3_interactive_html_dashboard.py" to HTML_DASHBOARD_SCRIPT
    )
}
