package com.valathi.mifid.pocketmonitor;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.valathi.mifid.pocketmonitor.collector.DiagnosticRepository;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG_UI = "MIFID-UI";

    // Durations: Screen 1 (Home Dashboard) = ~12s, Screens 2, 3, 4 = ~8s
    public static final long PAGE1_DURATION_MS = 12000L;
    public static final long PAGE_STANDARD_DURATION_MS = 8000L;
    private static final int TOTAL_PAGES = 4;

    private int currentPage = 0;
    private int clientRotationIndex = 0;
    private boolean isPaused = false;
    private final Handler handler = new Handler();
    private DiagnosticRepository repository;

    // Header views
    private TextView tvHeaderPage;
    private TextView tvHeaderSubtitle;
    private TextView tvHeaderSignal;
    private ImageView ivCarrierLogo;
    private TextView tvHeaderBattery;

    // Screen 1 Container & Box Views
    private View layoutScreen1Boxes;
    private TextView tvBoxCellTitle;
    private TextView tvBoxCellRsrp;
    private TextView tvBoxCellRoam;
    private TextView tvBoxHotspotState;
    private TextView tvBoxHotspotClients;
    private TextView tvBoxHotspotSsid;
    private TextView tvBoxHotspotPwd;
    private TextView tvBoxDataTraffic;
    private TextView tvBoxBatteryLevel;
    private TextView tvBoxBatteryStatus;

    // Screen 2 Container & Lines Views
    private View layoutScreen2Lines;
    private TextView tvLine1;
    private TextView tvLine2;
    private TextView tvLine3;
    private TextView tvLine4;
    private TextView tvLine5;
    private TextView tvLine6;
    private TextView tvLine7;

    // Screen 3 Container & Hotspot Client Views
    private View layoutScreen3Clients;
    private TextView tvScreen3Count;
    private View boxClientDetails;
    private TextView tvClientIndex;
    private TextView tvClientHostname;
    private TextView tvClientIp;
    private TextView tvClientMac;
    private TextView tvClientEmpty;

    // Screen 4 Container & System Diagnostics Views
    private View layoutScreen4System;
    private TextView tvSysLine1;
    private TextView tvSysLine2;
    private TextView tvSysLine3;
    private TextView tvSysLine4;
    private TextView tvSysLine5;
    private TextView tvSysLine6;
    private TextView tvSysLine7;

    private final Runnable rotationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPaused) return;

            // Advance to next page in sequence
            currentPage = (currentPage + 1) % TOTAL_PAGES;

            // Refresh data periodically
            repository.refresh(MainActivity.this);

            // Display page
            renderPage(currentPage);

            // Schedule next rotation with page-specific duration (12s for Page 1, 8s for Pages 2, 3, 4)
            long nextDelay = (currentPage == 0) ? PAGE1_DURATION_MS : PAGE_STANDARD_DURATION_MS;
            Log.i(TAG_UI, "Scheduled rotation to page " + (currentPage + 1) + " for " + (nextDelay / 1000) + "s");
            handler.postDelayed(this, nextDelay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        applyImmersiveMode();

        // Bind Header Views
        tvHeaderPage = (TextView) findViewById(R.id.tv_header_page);
        tvHeaderSubtitle = (TextView) findViewById(R.id.tv_header_subtitle);
        tvHeaderSignal = (TextView) findViewById(R.id.tv_header_signal);
        ivCarrierLogo = (ImageView) findViewById(R.id.iv_carrier_logo);
        tvHeaderBattery = (TextView) findViewById(R.id.tv_header_battery);

        // Bind Screen 1 Box Views
        layoutScreen1Boxes = findViewById(R.id.layout_screen1_boxes);
        tvBoxCellTitle = (TextView) findViewById(R.id.tv_box_cell_title);
        tvBoxCellRsrp = (TextView) findViewById(R.id.tv_box_cell_rsrp);
        tvBoxCellRoam = (TextView) findViewById(R.id.tv_box_cell_roam);
        tvBoxHotspotState = (TextView) findViewById(R.id.tv_box_hotspot_state);
        tvBoxHotspotClients = (TextView) findViewById(R.id.tv_box_hotspot_clients);
        tvBoxHotspotSsid = (TextView) findViewById(R.id.tv_box_hotspot_ssid);
        tvBoxHotspotPwd = (TextView) findViewById(R.id.tv_box_hotspot_pwd);
        tvBoxDataTraffic = (TextView) findViewById(R.id.tv_box_data_traffic);
        tvBoxBatteryLevel = (TextView) findViewById(R.id.tv_box_battery_level);
        tvBoxBatteryStatus = (TextView) findViewById(R.id.tv_box_battery_status);

        // Bind Screen 2 Lines Views
        layoutScreen2Lines = findViewById(R.id.layout_screen2_lines);
        tvLine1 = (TextView) findViewById(R.id.tv_line1);
        tvLine2 = (TextView) findViewById(R.id.tv_line2);
        tvLine3 = (TextView) findViewById(R.id.tv_line3);
        tvLine4 = (TextView) findViewById(R.id.tv_line4);
        tvLine5 = (TextView) findViewById(R.id.tv_line5);
        tvLine6 = (TextView) findViewById(R.id.tv_line6);
        tvLine7 = (TextView) findViewById(R.id.tv_line7);

        // Bind Screen 3 Hotspot Client Views
        layoutScreen3Clients = findViewById(R.id.layout_screen3_clients);
        tvScreen3Count = (TextView) findViewById(R.id.tv_screen3_count);
        boxClientDetails = findViewById(R.id.box_client_details);
        tvClientIndex = (TextView) findViewById(R.id.tv_client_index);
        tvClientHostname = (TextView) findViewById(R.id.tv_client_hostname);
        tvClientIp = (TextView) findViewById(R.id.tv_client_ip);
        tvClientMac = (TextView) findViewById(R.id.tv_client_mac);
        tvClientEmpty = (TextView) findViewById(R.id.tv_client_empty);

        // Bind Screen 4 System Diagnostics Views
        layoutScreen4System = findViewById(R.id.layout_screen4_system);
        tvSysLine1 = (TextView) findViewById(R.id.tv_sys_line1);
        tvSysLine2 = (TextView) findViewById(R.id.tv_sys_line2);
        tvSysLine3 = (TextView) findViewById(R.id.tv_sys_line3);
        tvSysLine4 = (TextView) findViewById(R.id.tv_sys_line4);
        tvSysLine5 = (TextView) findViewById(R.id.tv_sys_line5);
        tvSysLine6 = (TextView) findViewById(R.id.tv_sys_line6);
        tvSysLine7 = (TextView) findViewById(R.id.tv_sys_line7);

        repository = new DiagnosticRepository();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        applyImmersiveMode();

        // Refresh and display Screen 1 (Home Dashboard) immediately
        currentPage = 0;
        repository.refresh(this);
        renderPage(currentPage);

        // Ensure no duplicate timers exist, then schedule Screen 1 for full 12 seconds
        handler.removeCallbacks(rotationRunnable);
        Log.i(TAG_UI, "onResume: Screen 1 started for " + (PAGE1_DURATION_MS / 1000) + "s");
        handler.postDelayed(rotationRunnable, PAGE1_DURATION_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        // Stop polling/rotation to conserve CPU and power while display is off/paused
        handler.removeCallbacks(rotationRunnable);
        Log.i(TAG_UI, "onPause: rotation suspended");
    }

    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= 19) {
            try {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } catch (Throwable ignored) {}
        }
    }

    private void renderPage(int page) {
        tvHeaderPage.setText((page + 1) + "/" + TOTAL_PAGES);
        updateTopStatusBar();

        if (layoutScreen1Boxes != null) layoutScreen1Boxes.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        if (layoutScreen2Lines != null) layoutScreen2Lines.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        if (layoutScreen3Clients != null) layoutScreen3Clients.setVisibility(page == 2 ? View.VISIBLE : View.GONE);
        if (layoutScreen4System != null) layoutScreen4System.setVisibility(page == 3 ? View.VISIBLE : View.GONE);

        switch (page) {
            case 0:
                renderScreen1Dashboard();
                break;
            case 1:
                renderScreen2NetworkDetails();
                break;
            case 2:
                renderScreen3HotspotClients();
                break;
            case 3:
                renderScreen4System();
                break;
        }
    }

    /**
     * Updates the persistent top status bar visible across all screens:
     * - Network signal bars (e.g. [||||], [|||.], [||..], [|...], [....])
     * - Carrier icon (Airtel, Jio, Vodafone, Android, Termux)
     * - Battery level and charging indicator (e.g. 100% or ⚡99%)
     */
    private void updateTopStatusBar() {
        if (tvHeaderSignal != null) {
            DiagnosticItem sigBars = repository.findItem("CELLULAR", "Signal Bars");
            String bars = sigBars != null ? sigBars.getValue() : "--";
            if ("--".equals(bars)) {
                tvHeaderSignal.setText("[....]");
            } else {
                tvHeaderSignal.setText(bars);
            }
        }

        if (ivCarrierLogo != null) {
            DiagnosticItem op = repository.findItem("CELLULAR", "Operator");
            String opName = op != null ? op.getValue() : "";
            int logoRes = getCarrierLogoResource(opName);
            ivCarrierLogo.setImageResource(logoRes);
        }

        if (tvHeaderBattery != null) {
            DiagnosticItem batLevel = repository.findItem("BATTERY", "Level");
            DiagnosticItem batStatus = repository.findItem("BATTERY", "Status");
            String bPct = batLevel != null ? batLevel.getValue() : "--%";
            String bStat = batStatus != null ? batStatus.getValue() : "";
            boolean isCharging = "CHARGING".equalsIgnoreCase(bStat);
            // Append small charging indicator if charging
            tvHeaderBattery.setText(isCharging ? ("⚡" + bPct) : bPct);
        }
    }

    private int getCarrierLogoResource(String opName) {
        if (opName == null) return R.drawable.ic_carrier_android_mono;
        String lower = opName.toLowerCase();
        if (lower.contains("airtel")) {
            return R.drawable.ic_carrier_airtel;
        } else if (lower.contains("jio")) {
            return R.drawable.ic_carrier_jio;
        } else if (lower.contains("vodafone") || lower.contains("vi") || lower.contains("idea")) {
            return R.drawable.ic_carrier_vodafone;
        } else if (lower.contains("termux")) {
            return R.drawable.ic_carrier_termux;
        } else if (lower.contains("android")) {
            return R.drawable.ic_carrier_android;
        }
        return R.drawable.ic_carrier_android_mono;
    }

    /**
     * Screen 1 — Primary Home Status Dashboard:
     * Compact status boxes displaying the 4 core categories:
     * A. CELLULAR NETWORK: Operator, Tech, Signal Bars, RSRP dBm, Roaming YES/NO
     * B. HOTSPOT: State ON/OFF, Connected clients count (e.g. CLIENTS 01)
     * C. MOBILE DATA: Session / Boot usage: DL xxxMB, UL xxxMB
     * D. BATTERY: Percentage & validated charging state (CHARGING, FULL, NOT CHARGING)
     */
    private void renderScreen1Dashboard() {
        tvHeaderSubtitle.setText("MODEM DASHBOARD");

        // Category A: CELLULAR
        DiagnosticItem op = repository.findItem("CELLULAR", "Operator");
        DiagnosticItem netType = repository.findItem("CELLULAR", "Network Type");
        DiagnosticItem sigStr = repository.findItem("CELLULAR", "Signal Strength");
        DiagnosticItem sigBars = repository.findItem("CELLULAR", "Signal Bars");
        DiagnosticItem roaming = repository.findItem("CELLULAR", "Roaming");

        // Format short carrier name & tech (e.g. "AIR 4G")
        String opStr = (op != null && op.getValue() != null) ? op.getValue().toUpperCase(Locale.US) : "AIRTEL";
        if (opStr.contains("AIRTEL")) opStr = "AIR";
        else if (opStr.contains("VODAFONE") || opStr.contains("IDEA")) opStr = "VI";
        else if (opStr.contains("RELIANCE") || opStr.contains("JIO")) opStr = "JIO";

        String typeStr = (netType != null && netType.getValue() != null) ? netType.getValue() : "LTE";
        if ("LTE".equalsIgnoreCase(typeStr)) typeStr = "4G";

        tvBoxCellTitle.setText(opStr + " " + typeStr);

        String sBars = (sigBars != null && sigBars.getValue() != null) ? sigBars.getValue() : "[....]";

        String sVal = (sigStr != null && sigStr.getValue() != null) ? sigStr.getValue() : "N/A";
        // Clean to simple "-84 dBm"
        if (sVal.contains("(")) {
            int idx = sVal.indexOf('(');
            sVal = sVal.substring(0, idx).trim();
        }
        tvBoxCellRsrp.setText(sVal);

        String roamVal = (roaming != null && roaming.getValue() != null) ? roaming.getValue() : "HOME";
        boolean isRoam = roamVal.contains("ON") || roamVal.contains("ROAMING");
        tvBoxCellRoam.setText(isRoam ? "ROAM YES" : "ROAM NO");

        // Category B: HOTSPOT
        DiagnosticItem wifi = repository.findItem("WIFI", "Radio Mode");
        DiagnosticItem clients = repository.findItem("WIFI", "Hotspot Clients");
        DiagnosticItem hotspotSsid = repository.findItem("WIFI", "Hotspot SSID");
        DiagnosticItem hotspotPwd = repository.findItem("WIFI", "Hotspot Password");
        boolean isApOn = wifi != null && wifi.getValue() != null && wifi.getValue().contains("ON");
        tvBoxHotspotState.setText(isApOn ? "HOTSPOT ON" : "HOTSPOT OFF");

        int clientCount = 0;
        try {
            if (clients != null && clients.getValue() != null) {
                clientCount = Integer.parseInt(clients.getValue().trim());
            }
        } catch (Throwable ignored) {}
        tvBoxHotspotClients.setText(String.format(Locale.US, "CLIENTS %02d", clientCount));

        String ssidStr = (hotspotSsid != null && hotspotSsid.getValue() != null) ? hotspotSsid.getValue() : "--";
        String pwdStr = (hotspotPwd != null && hotspotPwd.getValue() != null) ? hotspotPwd.getValue() : "--";
        tvBoxHotspotSsid.setText("ID: " + ssidStr);
        tvBoxHotspotPwd.setText("PW: " + pwdStr);

        // Category C: MOBILE DATA (DL & UL session traffic since boot)
        DiagnosticItem dataDl = repository.findItem("DATA", "Cellular DL");
        DiagnosticItem dataUl = repository.findItem("DATA", "Cellular UL");
        String dlStr = (dataDl != null && dataDl.getValue() != null) ? dataDl.getValue() : "N/A";
        String ulStr = (dataUl != null && dataUl.getValue() != null) ? dataUl.getValue() : "N/A";
        // E.g. "DL 4.3MB  UL 155MB" or "DL 0 B  UL 0 B"
        dlStr = dlStr.replace(" ", "");
        ulStr = ulStr.replace(" ", "");
        tvBoxDataTraffic.setText("DL " + dlStr + "  UL " + ulStr);

        // Category D: BATTERY
        DiagnosticItem batLevel = repository.findItem("BATTERY", "Level");
        DiagnosticItem batStatus = repository.findItem("BATTERY", "Status");
        String bPct = (batLevel != null && batLevel.getValue() != null) ? batLevel.getValue() : "100%";
        String bStat = (batStatus != null && batStatus.getValue() != null) ? batStatus.getValue() : "UNKNOWN";
        tvBoxBatteryLevel.setText("BAT " + bPct);
        tvBoxBatteryStatus.setText(bStat);

        Log.i(TAG_UI, "Screen1_Dashboard: [" + opStr + " " + typeStr + " " + sBars + " " + sVal + " roam=" + isRoam
                + "] [HOTSPOT=" + (isApOn ? "ON" : "OFF") + " clients=" + clientCount + "] [DL=" + dlStr + " UL=" + ulStr
                + "] [BAT=" + bPct + " " + bStat + "]");
    }

    /**
     * Screen 2 — Detailed Network & Diagnostics Lines:
     * - Line 1: Cellular Data Total (Session since boot)
     * - Line 2: Cellular WAN IP address (rmnet_data0)
     * - Line 3: Wi-Fi Hotspot LAN IP address (bridge1)
     * - Line 4: Default Gateway
     * - Line 5: Battery Temp & Voltage
     * - Line 6: Device Uptime (Trip duration)
     * - Line 7: Live Refresh Timestamp
     */
    private void renderScreen2NetworkDetails() {
        tvHeaderSubtitle.setText("CELL & NETWORK");

        DiagnosticItem cellId = repository.findItem("CELLULAR", "Cell ID");
        DiagnosticItem bsId = repository.findItem("CELLULAR", "BS ID / TAC");
        DiagnosticItem gw = repository.findItem("NETWORK", "Default GW");
        DiagnosticItem netType = repository.findItem("CELLULAR", "Network Type");

        String cellIp = extractCellularIp();
        String bridgeIp = findInterfaceDisplay("bridge1");

        // Line 1: Cell ID (Distinguished from PCI)
        String cidStr = (cellId != null && cellId.getValue() != null) ? cellId.getValue() : "N/A";
        tvLine1.setText("CELL ID: " + cidStr);

        // Line 2: BS ID / TAC (Documented as TAC/LAC tracking area)
        String bidStr = (bsId != null && bsId.getValue() != null) ? bsId.getValue() : "N/A";
        tvLine2.setText("BS/TAC:  " + bidStr);

        // Line 3: Cellular IP
        tvLine3.setText("WAN: " + (cellIp != null ? cellIp : "---.---.---.---"));

        // Line 4: LAN / Hotspot IP
        tvLine4.setText("LAN: " + (bridgeIp != null ? bridgeIp : "192.168.100.1"));

        // Line 5: Gateway
        tvLine5.setText("GW:  " + (gw != null ? gw.getValue() : "N/A"));

        // Line 6: DNS Server
        String dns1 = getSystemProperty("net.dns1");
        if (dns1 == null || dns1.isEmpty()) dns1 = getSystemProperty("net.rmnet_data0.dns1");
        tvLine6.setText("DNS: " + (dns1 != null && !dns1.isEmpty() ? dns1 : "1.1.1.1"));

        // Line 7: Radio Link State & Band
        String nType = (netType != null && netType.getValue() != null) ? netType.getValue() : "LTE";
        tvLine7.setText("LINK: " + nType + " | BAND: N/A");

        Log.i(TAG_UI, "Screen2_Details: cid=" + cidStr + " bsid=" + bidStr + " wan=" + cellIp + " lan=" + bridgeIp);
    }

    private String findInterfaceDisplay(String ifacePrefix) {
        List<DiagnosticItem> items = repository.getItems();
        for (DiagnosticItem item : items) {
            if ("INTERFACES".equals(item.getCategory())) {
                String name = item.getLabel();
                if (name != null && name.startsWith(ifacePrefix)) {
                    return item.getValue();
                }
            }
        }
        return null;
    }

    private String extractCellularIp() {
        String ip = findInterfaceDisplay("rmnet_data0");
        if (ip == null) ip = findInterfaceDisplay("rmnet0");

        if (ip != null && !ip.contains("UP (No IP)") && !ip.contains("DOWN")) {
            int comma = ip.indexOf(',');
            return (comma > 0) ? ip.substring(0, comma).trim() : ip.trim();
        }

        // Fallback to bridge1 or any other active interface
        String bridge = findInterfaceDisplay("bridge1");
        if (bridge != null && !bridge.contains("UP (No IP)") && !bridge.contains("DOWN")) {
            int comma = bridge.indexOf(',');
            return (comma > 0) ? bridge.substring(0, comma).trim() : bridge.trim();
        }

        return null;
    }

    /**
     * Screen 3 — Hotspot Connected Device Details:
     * Authoritative source: /data/misc/dhcp/dnsmasq.leases & /proc/net/arp
     * Displays hostname, IP address, and MAC address.
     * Rotates through connected clients if multiple are associated.
     */
    private void renderScreen3HotspotClients() {
        tvHeaderSubtitle.setText("HOTSPOT CLIENTS");

        List<com.valathi.mifid.pocketmonitor.collector.NetworkInfoCollector.HotspotClient> clients =
                com.valathi.mifid.pocketmonitor.collector.NetworkInfoCollector.getConnectedClients();

        int count = clients.size();
        tvScreen3Count.setText(String.format(Locale.US, "%02d", count));

        if (count == 0) {
            if (boxClientDetails != null) boxClientDetails.setVisibility(View.GONE);
            if (tvClientEmpty != null) tvClientEmpty.setVisibility(View.VISIBLE);
        } else {
            if (boxClientDetails != null) boxClientDetails.setVisibility(View.VISIBLE);
            if (tvClientEmpty != null) tvClientEmpty.setVisibility(View.GONE);

            // Cycle through clients across screen rotations
            int index = clientRotationIndex % count;
            clientRotationIndex++;

            com.valathi.mifid.pocketmonitor.collector.NetworkInfoCollector.HotspotClient client = clients.get(index);
            tvClientIndex.setText("CLIENT " + (index + 1) + " OF " + count);
            tvClientHostname.setText("HOST: " + client.getHostname());
            tvClientIp.setText("IP:   " + client.getIp());
            tvClientMac.setText("MAC:  " + client.getMac());
        }

        Log.i(TAG_UI, "Screen3_HotspotClients: total=" + count);
    }

    /**
     * Screen 4 — Hardware & System Diagnostics:
     * Displays SoC, OS version, Memory stats, Battery details, and Uptime.
     */
    private void renderScreen4System() {
        tvHeaderSubtitle.setText("SYSTEM STATUS");

        DiagnosticItem osItem = repository.findItem("SYSTEM", "Android Ver");
        if (osItem == null) osItem = repository.findItem("SYSTEM", "OS");

        DiagnosticItem socItem = repository.findItem("CPU", "Hardware");
        if (socItem == null) socItem = repository.findItem("SYSTEM", "SoC");

        DiagnosticItem availRam = repository.findItem("MEMORY", "Avail RAM");
        DiagnosticItem totalRam = repository.findItem("MEMORY", "Total RAM");
        DiagnosticItem kernelTotal = repository.findItem("MEMORY", "Kernel Total");

        String ramStr;
        if (availRam != null && totalRam != null) {
            ramStr = availRam.getValue() + " / " + totalRam.getValue();
        } else if (availRam != null && kernelTotal != null) {
            String kt = kernelTotal.getValue().replace(" kB", "k").trim();
            ramStr = availRam.getValue() + " / " + kt;
        } else if (totalRam != null) {
            ramStr = totalRam.getValue();
        } else if (availRam != null) {
            ramStr = "Avail " + availRam.getValue();
        } else {
            ramStr = "N/A";
        }

        DiagnosticItem uptimeItem = repository.findItem("SYSTEM", "Uptime");
        DiagnosticItem cpuFreq = repository.findItem("CPU", "Frequency");
        DiagnosticItem cpuTemp = repository.findItem("CPU", "SoC Temp");
        DiagnosticItem storageItem = repository.findItem("STORAGE", "Internal Free");

        String cpuDetail = (cpuFreq != null ? cpuFreq.getValue() : "800 MHz");
        if (cpuTemp != null) cpuDetail += " (" + cpuTemp.getValue() + ")";

        DiagnosticItem imeiItem = repository.findItem("CELLULAR", "IMEI");
        DiagnosticItem imsiItem = repository.findItem("CELLULAR", "IMSI");
        DiagnosticItem iccidItem = repository.findItem("CELLULAR", "ICCID");

        String imeiStr = (imeiItem != null && imeiItem.getValue() != null) ? imeiItem.getValue() : "N/A";
        String imsiStr = (imsiItem != null && imsiItem.getValue() != null) ? imsiItem.getValue() : "N/A";
        String iccidStr = (iccidItem != null && iccidItem.getValue() != null) ? iccidItem.getValue() : "N/A";

        tvSysLine1.setText("OS:  " + (osItem != null ? ("Android " + osItem.getValue()) : "Android 4.4.4"));
        tvSysLine2.setText("SOC: " + (socItem != null ? socItem.getValue() : "MSM8916"));
        tvSysLine3.setText("CPU: " + cpuDetail);
        tvSysLine4.setText("RAM: " + ramStr);
        tvSysLine5.setText("IMEI:" + imeiStr);
        tvSysLine6.setText("IMSI:" + imsiStr);
        tvSysLine7.setText("ICC: " + iccidStr);

        Log.i(TAG_UI, "Screen4_System: ram=" + ramStr + " cpu=" + cpuDetail);
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method method = clazz.getMethod("get", String.class);
            return (String) method.invoke(null, key);
        } catch (Throwable ignored) {
            return null;
        }
    }
}