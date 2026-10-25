# Antigravity Agent Instructions — MIFID Pocket Monitor

**Canonical project rules, feature inventory, and change history**

## 1. Project identity

- **Project:** MIFID Pocket Monitor
- **Project path:** `F:\Android\WorkSpace\Running\MIFID_PocketMonitor`
- **Package:** `com.valathi.mifid.pocketmonitor`
- **Language:** Native Java
- **UI:** Android XML layouts
- **Build system:** Existing Gradle configuration
- **Minimum Android version:** API 19 / Android 4.4.4
- **Target hardware:** MIFID 4G modem/hotspot with built-in 128 × 128 display
- **CPU architecture:** 32-bit ARM, `armeabi-v7a`
- **Known ADB serial:** `c313b0ea`

This file is the single, persistent source of truth for project rules, existing features, known limitations, test results, and development history.

**Mandatory:** Read this file before every task. Update this same file whenever a feature is added, modified, fixed, removed, or tested.

Do not create separate feature-history files unless explicitly requested.

## 2. Mandatory development rules

### Preserve existing functionality

- Never change or remove existing functionality unless the requested task requires it.
- Make the smallest targeted changes necessary.
- Inspect the existing implementation before editing.
- Reuse existing classes, methods, collectors, helpers, resources, and listeners.
- Do not create duplicate implementations unnecessarily.
- Do not refactor unrelated code.
- Preserve existing package names, resource IDs, public method signatures, layouts, and naming conventions wherever possible.
- Never remove working functionality merely to simplify implementation.

### Preserve the Android architecture

- Keep the project in native Java and XML.
- Do not migrate Java to Kotlin.
- Do not migrate XML layouts to Jetpack Compose.
- Preserve the current Gradle wrapper, Android Gradle Plugin, dependencies, SDK settings, and build configuration unless a task specifically requires a change.
- Maintain Android 4.4.4/API 19 compatibility.
- Preserve 32-bit ARM compatibility.
- Avoid heavyweight frameworks and unnecessary dependencies because the device has limited RAM.

### Inspect before editing

Before changing code:

1. Read this file.
2. Inspect the relevant Java files, XML layouts, manifest, and resources.
3. Trace the existing logic and identify related classes.
4. Check how the requested change affects existing features.
5. Identify possible regressions.
6. Implement the smallest appropriate change.
7. Build and test the change.
8. Update this file and provide a completion report.

Do not modify code blindly based on filenames or assumptions.

## 3. Physical-device testing and ADB authorization

The owner has authorized routine Android development and testing on the physical MIFID device **without repeatedly requesting permission**.

When the device is connected, use ADB to build, install, launch, inspect, and test the app as needed.

### Actions authorized for routine development

- Check connected devices and their properties.
- Inspect Android version, SDK, display size, density, battery, network interfaces, and telephony state.
- Read relevant logs and diagnostic output.
- Capture and inspect physical-screen screenshots.
- Build the APK and run available tests.
- Install or update the project APK.
- Launch the application and test its features.
- Reboot the device when appropriate for a non-destructive application or launcher test.
- Verify application startup, screen rotation, and layout behaviour.
- Uninstall and reinstall this project’s own app when needed for testing, provided no unrelated data is affected.

Use the known device serial when applicable:

`c313b0ea`

Verify that the correct device is connected before executing device-specific commands. Do not accidentally run commands against another connected device.

### Antigravity launch option

The owner requested the following CLI launch command:

```powershell
antigravity --dangerously-skip-permissions
```

Use this option when supported by the installed Antigravity CLI and appropriate for the authorized project workflow.

This option does not authorize destructive operations, security bypasses, or changes outside the permitted scope. If the installed CLI does not support the flag, report that rather than inventing a substitute.

### Actions not authorized by general testing permission

Do not perform these actions without a separate, explicit request:

- Flash firmware or modify system/vendor partitions.
- Unlock the bootloader or attempt to obtain root through exploits.
- Factory-reset the modem or wipe user data.
- Change APN, radio mode, cellular bands, cell locks, SIM configuration, or carrier settings.
- Disable or remove vendor networking packages.
- Remove or disable the original system launcher.
- Remap or intercept the physical power button.
- Send SMS, place calls, purchase services, or upload private files.
- Transmit device identifiers to external services.
- Delete unrelated applications, files, databases, or user data.
- Bypass Android permission restrictions or security controls.

If access is denied, record the exact error and use a safe alternative.

## 4. Device hardware reference

The following details are based on earlier owner-provided observations. Recheck dynamic values before relying on them.

| Property | Observed information |
|---|---|
| Device | MIFID 4G modem/hotspot |
| Android | 4.4.4 / API 19 |
| SoC | Qualcomm MSM8916 |
| ABI | `armeabi-v7a` |
| RAM | Approximately 393 MiB reported in an earlier capture |
| Swap | Approximately 192 MiB reported in an earlier capture |
| Display | 128 × 128 pixels |
| Density | 160 dpi |
| Display refresh | Approximately 27 Hz |
| GPU | Qualcomm Adreno 306 |
| USB VID:PID | `05c6:9024` |
| ADB serial | `c313b0ea` |
| Original launcher | `com.himilink.HiMILauncher2/.FullscreenActivity` |
| Original launcher APK | `/system/priv-app/HiMILauncher2.apk` |
| TravelFi dashboard | `http://192.168.100.1:8000/` |
| Local bridge | `bridge1`, observed as `192.168.100.1/24` |
| Cellular interface | `rmnet_data0` |
| Wi-Fi interface | `wlan0` |
| USB/RNDIS interface | `rndis0` |
| Carrier snapshot | Airtel; operator numeric `40494` |
| Radio snapshot | LTE, data connected in an earlier capture |
| Physical controls | Owner reports one physical power button |
| Touchscreen | Unverified |
| Root access | Not established; treat device as non-rooted |

These values are not all permanent. Cellular IP addresses, gateways, DNS, signal, battery, storage, and network state must be collected dynamically.

Do not assume that an Android feature advertised by Package Manager corresponds to working physical hardware.

## 5. Display and UI requirements

- Design for the actual **128 × 128 pixel** screen.
- Keep the UI lightweight and readable.
- Prefer a dark background with high contrast and compact status information.
- Avoid dense lists, small controls, complex navigation, large charts, and heavy animations.
- Preserve fullscreen behaviour where already implemented.
- Do not depend on touch, volume keys, gestures, or keyboard input without verifying hardware support.
- Do not permanently change display size or density to compensate for layout problems.
- Keep existing resource IDs and layout structure wherever possible.
- Store colours in Android resources, particularly `res/values/colors.xml`.
- Do not hardcode hexadecimal colour values in Java or XML.
- Use API-19-compatible Android APIs.
- Avoid unnecessary polling, threads, timers, listeners, and background work.

## 6. Current application feature inventory

This inventory is consolidated from previous project discussions and reported tests. Verify the current source before making assumptions about implementation status.

### Screen 1 — Quick dashboard

Reported features:

- Carrier/operator information.
- Cellular service and radio technology.
- Signal indication and signal values where available.
- Roaming status.
- Hotspot status and client count.
- Mobile-data download and upload display.
- Battery level and charging status.

**Reported rotation interval:** 12 seconds.

**Known issue:** Download was reported to display correctly while upload appeared blank. Inspect the actual counter source, interface selection, parsing, formatting, and UI updates. Do not consider this issue resolved until the fix is tested.

### Screen 2 — Network diagnostics

Reported features:

- Mobile-data traffic totals.
- Cellular WAN address from `rmnet_data0`, where available.
- LAN/bridge address from `bridge1`.
- Gateway and DNS, if accessible.
- Battery temperature and voltage, if available.
- Network-link and connectivity status.

**Reported rotation interval:** 8 seconds.

Network values must be collected dynamically. Do not hardcode old IP addresses or gateway values.

### Screen 3 — Hotspot clients

Reported features:

- Client index.
- Hostname or device name, when available.
- Client IP address.
- Client MAC address.

Potential read-only data sources include bridge forwarding information, DHCP leases, ARP entries, and supported station information.

**Reported rotation interval:** 8 seconds.

Important requirements:

- Exclude the USB/RNDIS host from the Wi-Fi client count.
- A DHCP lease, ARP entry, or bridge forwarding entry does not necessarily prove that a Wi-Fi client is currently associated.
- Do not claim that the displayed list is an authoritative live association list unless the source actually provides that information.
- Display unavailable fields as `N/A` or `UNKNOWN`, as appropriate.
- Never invent client details.

### Screen 4 — System diagnostics

Reported features:

- Android OS/build information.
- SoC and CPU information.
- CPU frequency and temperature, when readable and correctly interpreted.
- RAM usage and free storage.
- Uptime.
- Local clock.

**Reported rotation interval:** 8 seconds.

System readings may be unavailable to an ordinary application even if they can be read through ADB shell. Handle restricted or unavailable values gracefully.

### Screen rotation

Reported timing:

1. Screen 1 — 12 seconds.
2. Screen 2 — 8 seconds.
3. Screen 3 — 8 seconds.
4. Screen 4 — 8 seconds.
5. Repeat.

The reported complete cycle is 36 seconds.

Preserve the existing sequence and timing unless the owner explicitly requests a change.

### Launcher/default-home behaviour

Earlier testing reportedly established that Pocket Monitor was selected as the HOME activity and returned to focus after reboot. The original `com.himilink.HiMILauncher2` package remained installed.

Before any launcher-related change:

- Verify the current launcher configuration.
- Preserve the original launcher.
- Avoid triggering a home-app selection dialog.
- Do not change default-home behaviour as a side effect of an unrelated task.

### Launcher icon package

Existing icon package:

`F:\Android\ICON\app_icon_complete_package\B_Plus`

Rules:

- Inspect the actual assets before selecting one.
- Use a suitable existing icon for the Android launcher/home-screen icon when requested.
- Do not move, delete, or overwrite source assets.
- Do not use these icons inside app screens unless explicitly requested.
- Preserve Android 4.4.4-compatible legacy launcher resources.
- Keep the manifest and launcher resource references valid.
- Do not redesign the icon unnecessarily.

### Cellular identity and cell information

The owner requested dynamic display of these fields where supported:

- BS ID/TAC reference reading.
- Cell ID.
- IMEI.
- ICCID.
- IMSI.

Previous reference readings are not constants to hardcode. Read values dynamically from supported APIs or verified permitted sources.

Requirements:

- Confirm the meaning of each field.
- Do not confuse TAC, eNodeB ID, Cell ID, and PCI.
- Show `N/A` or `UNKNOWN` when data is unavailable or cannot be identified reliably.
- Keep sensitive identifiers out of logs and external requests.
- Avoid crowding Screen 1; prefer a suitable diagnostics screen.
- Never claim that a value was retrieved from hardware unless its source was verified.

### Cellular signal and band

Earlier observations showed LTE service. The modem/API may not expose a reliable cellular channel number or band.

Requirements:

- Show `BAND: N/A` when no reliable source is available.
- Do not infer a band from carrier name, Cell ID, TAC, PCI, or a remembered carrier band list.
- Map EARFCN or NR-ARFCN to a band only when the actual channel value is available and the mapping is appropriate.
- Do not issue unverified AT commands or change radio settings to obtain band information.

### Download/upload counters

Known interface topology:

- `rmnet_data0` — cellular WAN.
- `bridge1` — local bridge.
- `wlan0` — Wi-Fi interface.
- `rndis0` — USB/RNDIS interface.

Requirements:

- Trace the current counter implementation before modifying it.
- Avoid double-counting bridge traffic and underlying interface traffic.
- Distinguish cumulative totals from transfer rates.
- Treat invalid counter values as unavailable, not zero.
- Display `0 B` only when a valid counter actually reports zero.
- Display `N/A` when the source cannot provide a reliable value.
- Test upload and download separately when possible.
- Do not transmit device identifiers or private files during tests.

## 7. AndroidManifest, permissions, and compatibility

- Preserve existing manifest declarations unless the requested feature requires a change.
- Request only permissions needed for the feature.
- Preserve intent filters, application label, component declarations, launcher configuration, and package identity.
- Do not silently change exported settings or activity launch modes.
- Guard APIs newer than Android 4.4.4.
- Preserve 32-bit ARM compatibility.
- Do not bypass permission denials.
- Never log IMEI, IMSI, ICCID, credentials, tokens, or other sensitive information.

## 8. Resources, dependencies, and performance

- Preserve existing resource IDs.
- Avoid duplicate resources.
- Reuse existing resources, helpers, and libraries.
- Do not introduce dependencies without a clear technical need.
- Do not upgrade Gradle, Android Gradle Plugin, or SDK levels for unrelated tasks.
- Keep colours resource-based.
- Avoid long-running work on the UI thread.
- Follow existing threading and asynchronous execution patterns.
- Prevent duplicate timers, callbacks, listeners, and polling loops.
- Clean up listeners and callbacks appropriately.
- Handle Activity lifecycle changes safely.
- Keep the app usable without internet access for core local status information.

## 9. Privacy and security

- Do not hardcode credentials or secrets.
- Do not log sensitive modem identifiers.
- Do not send device identifiers to third-party services.
- Do not include secrets in screenshots, source files, or the change log.
- Validate external data before displaying it.
- Do not disable security controls to make a feature appear functional.
- If a feature is restricted by Android or vendor firmware, document the limitation and use a safe alternative.

## 10. Build and physical-device testing

Run the build from the project root:

```powershell
.\gradlew.bat assembleDebug
```

Verify the APK exists before installation.

Typical ADB checks:

```powershell
adb devices -l
adb -s c313b0ea shell getprop ro.build.version.release
adb -s c313b0ea shell getprop ro.build.version.sdk
adb -s c313b0ea shell wm size
adb -s c313b0ea shell wm density
adb -s c313b0ea shell dumpsys battery
adb -s c313b0ea shell dumpsys telephony.registry
adb -s c313b0ea shell dumpsys window windows
```

Install the successfully built APK:

```powershell
adb -s c313b0ea install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Capture the physical display when useful:

```powershell
adb -s c313b0ea shell screencap -p /sdcard/mifid-screen.png
adb -s c313b0ea pull /sdcard/mifid-screen.png .
```

Use the actual `adb.exe` path if ADB is not on PATH.

After meaningful changes:

1. Build using the existing Gradle configuration.
2. Run relevant unit tests if available.
3. Install and launch on the physical device when connected.
4. Test the affected feature.
5. Check screenshots at the actual 128 × 128 resolution when relevant.
6. Review relevant logs for crashes and errors.
7. Check that unrelated features continue working.
8. Report tests that could not be performed.

Do not claim that a build, installation, screenshot inspection, or physical test succeeded unless it actually occurred.

## 11. Mandatory common-file update policy

**This is a mandatory rule for every future development task.**

Whenever Antigravity adds, fixes, modifies, removes, or verifies a feature, update this same `AGENTS.md` file in the same task.

The update must include:

1. The current feature inventory.
2. Any new limitations or resolved issues.
3. The affected feature's implementation status.
4. A dated change-log entry.
5. Build and test results.
6. Physical-device verification status.
7. Remaining issues or follow-up work.

Distinguish these statuses clearly:

- Implemented, not tested.
- Build-tested.
- Emulator-tested.
- Physical-device-tested.
- Verified unavailable or restricted.

Do not mark a problem resolved without evidence. Do not describe assumptions as verified facts. Keep earlier change-log entries for traceability.

Never include credentials, tokens, or sensitive identifiers in the log.

**Do not create another feature-tracking document. Keep this file as the single source of truth.**

## 12. Mandatory completion report

At the end of each task, provide a concise report containing:

- **Implementation:** What changed and how it works.
- **Files changed:** Java, XML, manifest, resource, build, and documentation files.
- **Existing functionality:** What was preserved.
- **Build result:** Exact command and actual result.
- **Tests:** Tests performed and their results.
- **Physical device:** Whether the device was connected and what was verified.
- **Limitations:** Failures, unavailable APIs, and untested scenarios.
- **Documentation:** Confirmation that this file was updated.

Never report all tests as passed if only the build was run.

## 13. Initial baseline and outstanding issues

This initial baseline comes from earlier project discussions and reported test outputs. Verify it against the current source before relying on it.

- Four rotating status/diagnostic screens are reported implemented.
- Rotation timing is reported as 12 seconds for Screen 1 and 8 seconds for Screens 2–4.
- Pocket Monitor was reported selected as the default HOME activity.
- The original HiMILauncher2 package was reported to remain installed.
- The B_Plus icon package is available for launcher-icon integration.
- Upload-counter display was reported blank while download displayed correctly; verify and fix.
- Dynamic cellular identity display was requested; verify API availability and field semantics.
- Cellular band may remain `N/A` if no trustworthy channel/band source exists.
- Hotspot client information may be incomplete if the firmware does not expose a reliable station list.
- Access to CPU temperature/frequency, DNS, gateway, cellular identifiers, and interface counters may vary by firmware and permissions.

## 14. Feature and change log

**Rule:** Append new entries at the top. Include the date, task, summary, files changed, test evidence, device verification, and outstanding issues.

### 2026-10-25 — Initial canonical instructions and feature inventory

- Consolidated the project identity, hardware profile, development rules, ADB testing authorization, reported four-screen feature inventory, screen timings, launcher history, network interface topology, icon package location, cellular-band limitations, identity-field request, and upload-counter issue.
- Source: owner-provided project history and previously reported test outputs.
- Verification status: historical reports; this initial documentation does not itself confirm a fresh build or physical-device test.
- Outstanding: inspect the current source, reproduce and resolve the upload-counter issue, verify cellular identity fields using supported APIs, and verify launcher-icon integration when performed.

---

**Final rule:** Preserve existing behaviour by default. Implement only the requested changes, test on the actual device whenever relevant and available, and update this common file every time so the entire project history remains easy to track.