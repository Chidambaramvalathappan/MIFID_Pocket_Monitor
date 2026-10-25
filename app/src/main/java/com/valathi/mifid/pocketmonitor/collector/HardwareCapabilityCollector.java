package com.valathi.mifid.pocketmonitor.collector;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.io.File;
import java.util.List;

public class HardwareCapabilityCollector implements InfoCollector {

    private static final String TAG_BATTERY = "MIFID-BAT";

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        // Battery
        collectBattery(context, items);

        // Sensors
        collectSensors(context, items);

        // Camera
        collectCamera(context, items);

        // Location / GPS
        collectLocation(context, items);

        // Bluetooth
        collectBluetooth(context, items);

        // Audio
        collectAudio(context, items);

        // System Uptime & Root Diagnostic
        collectSystemState(items);
    }

    private void collectBattery(Context context, List<DiagnosticItem> items) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                int pct = -1;
                if (level >= 0 && scale > 0) {
                    pct = (level * 100) / scale;
                    items.add(new DiagnosticItem("BATTERY", "Level", pct + "%", DiagnosticItem.Status.DETECTED));
                } else {
                    items.add(new DiagnosticItem("BATTERY", "Level", "N/A", DiagnosticItem.Status.UNAVAILABLE));
                }

                // Battery visual bar: [====], [===.], [==..], [=...], [....]
                String batBars = "--";
                if (pct >= 0) {
                    if (pct >= 80) batBars = "[====]";
                    else if (pct >= 60) batBars = "[===.]";
                    else if (pct >= 40) batBars = "[==..]";
                    else if (pct >= 20) batBars = "[=...]";
                    else batBars = "[....]";
                }
                items.add(new DiagnosticItem("BATTERY", "Bars", batBars, DiagnosticItem.Status.DETECTED));

                // Charging State: CHARGING, FULL, NOT CHARGING, DISCHARGING, UNKNOWN
                String statusStr = "UNKNOWN";
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    statusStr = "CHARGING";
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    statusStr = "FULL";
                } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    statusStr = "NOT CHARGING";
                } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    statusStr = "DISCHARGING";
                }
                items.add(new DiagnosticItem("BATTERY", "Status", statusStr, DiagnosticItem.Status.REPORTED));

                // Plugged source
                String pluggedStr = "UNPLUGGED";
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC) pluggedStr = "AC";
                else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) pluggedStr = "USB";
                else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) pluggedStr = "WIRELESS";
                items.add(new DiagnosticItem("BATTERY", "Power Source", pluggedStr, DiagnosticItem.Status.REPORTED));

                String tempStr = "N/A";
                if (temp > 0 && temp < 1000) { // e.g. 246 = 24.6 C
                    tempStr = String.format("%.1f C", temp / 10.0);
                    items.add(new DiagnosticItem("BATTERY", "Temp", tempStr, DiagnosticItem.Status.DETECTED));
                }

                if (voltage > 0) {
                    items.add(new DiagnosticItem("BATTERY", "Voltage", voltage + " mV", DiagnosticItem.Status.DETECTED));
                }

                Log.i(TAG_BATTERY, "level=" + pct + "% status=" + statusStr + " plugged=" + pluggedStr + " temp=" + tempStr + " voltage=" + voltage + "mV");
            } else {
                Log.w(TAG_BATTERY, "No battery broadcast received");
                items.add(new DiagnosticItem("BATTERY", "Service", "No battery broadcast", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            Log.e(TAG_BATTERY, "Error collecting battery info: " + t.getMessage(), t);
            items.add(new DiagnosticItem("BATTERY", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void collectSensors(Context context, List<DiagnosticItem> items) {
        try {
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) {
                List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);
                int count = (sensorList != null) ? sensorList.size() : 0;
                items.add(new DiagnosticItem("SENSORS", "Hardware Sensors", count > 0 ? String.valueOf(count) : "0 detected",
                        count > 0 ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.NOT_DETECTED));
            } else {
                items.add(new DiagnosticItem("SENSORS", "SensorService", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("SENSORS", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void collectCamera(Context context, List<DiagnosticItem> items) {
        try {
            PackageManager pm = context.getPackageManager();
            boolean hasCameraFeature = pm != null && (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                    pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));

            int numCameras = 0;
            try {
                numCameras = Camera.getNumberOfCameras();
            } catch (Throwable ignored) {}

            if (numCameras > 0) {
                items.add(new DiagnosticItem("CAMERA", "Devices", String.valueOf(numCameras), DiagnosticItem.Status.DETECTED));
            } else if (hasCameraFeature) {
                items.add(new DiagnosticItem("CAMERA", "Devices", "0 (Feature declared)", DiagnosticItem.Status.NOT_DETECTED));
            } else {
                items.add(new DiagnosticItem("CAMERA", "Devices", "None", DiagnosticItem.Status.NOT_DETECTED));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("CAMERA", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void collectLocation(Context context, List<DiagnosticItem> items) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                List<String> providers = lm.getAllProviders();
                boolean hasGps = providers != null && providers.contains(LocationManager.GPS_PROVIDER);
                boolean hasNet = providers != null && providers.contains(LocationManager.NETWORK_PROVIDER);

                if (hasGps) {
                    items.add(new DiagnosticItem("GPS", "Provider", "GPS Present (Hardware unverified)", DiagnosticItem.Status.NOT_VERIFIED));
                } else if (hasNet) {
                    items.add(new DiagnosticItem("GPS", "Provider", "Network Only", DiagnosticItem.Status.REPORTED));
                } else {
                    items.add(new DiagnosticItem("GPS", "Provider", "None detected", DiagnosticItem.Status.NOT_DETECTED));
                }
            } else {
                items.add(new DiagnosticItem("GPS", "LocationService", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("GPS", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void collectBluetooth(Context context, List<DiagnosticItem> items) {
        try {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba != null) {
                boolean enabled = ba.isEnabled();
                items.add(new DiagnosticItem("BLUETOOTH", "Radio", enabled ? "Enabled" : "Disabled (Radio unverified)", DiagnosticItem.Status.NOT_VERIFIED));
                items.add(new DiagnosticItem("BLUETOOTH", "Adapter", "Software Present", DiagnosticItem.Status.REPORTED));
            } else {
                items.add(new DiagnosticItem("BLUETOOTH", "Adapter", "Not available", DiagnosticItem.Status.NOT_DETECTED));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("BLUETOOTH", "Status", "Unverified / Restricted", DiagnosticItem.Status.NOT_VERIFIED));
        }
    }

    private void collectAudio(Context context, List<DiagnosticItem> items) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                boolean musicActive = am.isMusicActive();
                items.add(new DiagnosticItem("AUDIO", "Service", "Present", DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("AUDIO", "Music Active", String.valueOf(musicActive), DiagnosticItem.Status.DETECTED));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("AUDIO", "Status", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void collectSystemState(List<DiagnosticItem> items) {
        try {
            long uptimeMs = SystemClock.elapsedRealtime();
            long uptimeSec = uptimeMs / 1000;
            long hours = uptimeSec / 3600;
            long minutes = (uptimeSec % 3600) / 60;
            long seconds = uptimeSec % 60;
            items.add(new DiagnosticItem("SYSTEM", "Uptime", String.format("%02d:%02d:%02d", hours, minutes, seconds), DiagnosticItem.Status.DETECTED));
        } catch (Throwable ignored) {}

        // Root check (passive diagnostic only, no escalation)
        try {
            boolean suFound = false;
            String[] places = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su"};
            for (String p : places) {
                if (new File(p).exists()) {
                    suFound = true;
                    break;
                }
            }
            items.add(new DiagnosticItem("SECURITY", "su Binary", suFound ? "Found" : "Not Found",
                    suFound ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.NOT_DETECTED));
        } catch (Throwable ignored) {}
    }
}
