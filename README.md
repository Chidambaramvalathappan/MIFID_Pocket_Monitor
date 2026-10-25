# MIFID Pocket Monitor

**MIFID Pocket Monitor** is a specialized native Android system monitor and default launcher dashboard engineered specifically for portable Qualcomm Snapdragon (MSM8916) 4G LTE mobile Wi-Fi hotspots and travel modems equipped with tiny **128 × 128 pixel** displays running Android 4.4.4 (KitKat / API 19).

---

## 📱 Hardware Profile & Constraints

| Parameter | Specification |
|---|---|
| **Target Hardware** | MIFID 4G LTE Mobile Wi-Fi Hotspot / Modem |
| **SoC** | Qualcomm Snapdragon 410 (MSM8916) |
| **Architecture** | 32-bit ARM (`armeabi-v7a`) |
| **Operating System** | Android 4.4.4 KitKat (API 19) |
| **Display** | 128 × 128 pixels, 160 dpi, ~27 Hz refresh rate |
| **RAM / Swap** | ~393 MiB RAM / ~192 MiB Swap |
| **Network Interfaces** | `rmnet_data0` (Cellular WAN), `bridge1` (Local Bridge), `wlan0` (Wi-Fi AP), `rndis0` (USB RNDIS) |
| **Physical Controls** | Single physical power button (touchscreen unverified / headless) |
| **Root Status** | Non-rooted; operates strictly within standard Android API and permissions |

---

## 🖥️ Screen Overview (Auto-Rotating Carousel)

The dashboard continuously cycles through four focused diagnostic screens designed to fit legibly within the 128 × 128 pixel screen real estate:

```
+-------------------------------------------------------------+
| Screen 1 (12s) -> Screen 2 (8s) -> Screen 3 (8s) -> Screen 4 (8s) |
+-------------------------------------------------------------+
                      (Full Cycle: 36s)
```

### 1. Screen 1 — Quick Modem Dashboard (12 seconds)
- **Top Status Bar:** Dynamic signal bars (`[||||]`), carrier brand icon (Airtel, Jio, Vi, Android, Termux), battery percentage, charging state indicator, and page indicator (`1/4`).
- **Box A (Cellular):** Operator label (`AIR 4G`), live RSRP signal level in dBm (`-84 dBm`), and roaming status (`ROAM NO` / `ROAM YES`).
- **Box B (Wi-Fi Hotspot & Credentials):** Hotspot state (`HOTSPOT ON` / `HOTSPOT OFF`), active connected client count, Hotspot SSID name (`ID: <SSID>`), and pre-shared password (`PW: <Password>`).
- **Box C (Mobile Data Counters):** Independent session cellular traffic counters (`DL <Bytes>  UL <Bytes>`).
- **Box D (Battery Health):** Battery charge level and status (`BAT 99%  CHARGING`).

### 2. Screen 2 — Cell & Network Diagnostics (8 seconds)
- **Cell ID:** LTE Cell Identifier (CID), distinguished from PCI.
- **BS/TAC:** Base Station ID / Tracking Area Code (TAC/LAC).
- **WAN IP:** Live cellular WAN IP address from `rmnet_data0`.
- **LAN IP:** Hotspot local bridge IP address from `bridge1` (e.g. `192.168.100.1`).
- **Gateway & DNS:** Active default route gateway and upstream DNS resolvers (`net.dns1`).
- **Link State:** Active radio technology link state (`LINK: LTE | BAND: N/A`).

### 3. Screen 3 — Hotspot Connected Devices (8 seconds)
- **Client Count Header:** Total number of associated Wi-Fi devices.
- **Client Inspector:** Rotates through connected stations on each screen cycle, showing:
  - Client index (`CLIENT 1 OF N`)
  - Device Hostname / NetBIOS name
  - Client IPv4 address
  - Client Hardware MAC address
- **Interface Segregation:** Discerning Wi-Fi clients (`wlan0`, Port 1) from host USB tethering connections (`rndis0`, Port 2) via bridge forwarding inspection (`/sys/class/net/bridge1/brforward`), DHCP leases (`/data/misc/dhcp/dnsmasq.leases`), and ARP cache (`/proc/net/arp`).

### 4. Screen 4 — Hardware & System Status (8 seconds)
- **OS & Platform:** Android version and build release.
- **SoC & CPU:** Qualcomm MSM8916 hardware identification and live CPU frequency + SoC temperature.
- **RAM Memory:** Active memory utilization and total RAM.
- **Cellular Identifiers:** Dynamically extracted IMEI, SIM IMSI, and SIM ICCID numbers (queried securely with zero sensitive logging in logcat).

---

## 🎨 Design & Aesthetic System

- **Obsidian Workshop Theme:** Custom high-contrast dark and light palettes defined completely in `res/values/colors.xml` and `res/values-night/colors.xml` without hardcoded hex color literals in layout XML or Java code.
- **Typography:** Compact monospace 7sp and 8sp typography carefully tuned so every screen boundary stays strictly within `[0, 0][128, 128]`.
- **Launcher Integration:** Configured with `CATEGORY_HOME` and `CATEGORY_DEFAULT` intent filters to serve as the default home screen activity alongside `HiMILauncher2`.
- **Application Icon:** Uses the `B_Plus` icon set with adaptive icons and density-specific resources (`mdpi` through `xxxhdpi`).

---

## 🛠️ Build & Installation

### Prerequisites
- JDK 17 or compatible Java runtime
- Android SDK with Platform Tools and API 19 (KitKat) platform installed

### Building Debug APK
Run the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Installation via ADB
To install or update the app on the connected device:

```powershell
adb -s c313b0ea install -r app\build\outputs\apk\debug\app-debug.apk
```

To launch the activity:
```powershell
adb -s c313b0ea shell am start -n com.valathi.mifid.pocketmonitor/.MainActivity
```

---

## 🔒 Privacy & Permissions

- Requests standard read-only monitoring permissions:
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.ACCESS_WIFI_STATE`
  - `android.permission.READ_PHONE_STATE`
  - `android.permission.BLUETOOTH`
- **Zero Sensitive Logging:** Device hardware identifiers (IMEI, IMSI, ICCID, passwords) are strictly excluded from Android Logcat output.
- Non-destructive execution: Does not attempt root escalation, custom recovery modification, or NVRAM carrier alterations.

---

## 📄 License

Developed by **Chidambaramvalathappan**. All rights reserved.