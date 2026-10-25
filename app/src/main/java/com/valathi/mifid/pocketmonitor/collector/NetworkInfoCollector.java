package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NetworkInfoCollector implements InfoCollector {

    private static final String TAG = "MIFID-NET";
    private static final String TAG_WIFI = "MIFID-WIFI";

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        Log.i(TAG, "Scan started");

        // 1. ConnectivityManager
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo active = cm.getActiveNetworkInfo();
                if (active != null) {
                    items.add(new DiagnosticItem("NETWORK", "Active Conn", active.getTypeName() + " (" + active.getState() + ")", DiagnosticItem.Status.DETECTED));
                    items.add(new DiagnosticItem("NETWORK", "Subtype", active.getSubtypeName(), DiagnosticItem.Status.REPORTED));
                    items.add(new DiagnosticItem("NETWORK", "Connected", String.valueOf(active.isConnected()), DiagnosticItem.Status.DETECTED));
                    Log.i(TAG, "activeNetwork: " + active.getTypeName() + " state=" + active.getState() + " connected=" + active.isConnected());
                } else {
                    items.add(new DiagnosticItem("NETWORK", "Active Conn", "None / Disconnected", DiagnosticItem.Status.REPORTED));
                    Log.w(TAG, "activeNetwork: null (disconnected or handled via bridge/hotspot)");
                }

                NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (mobile != null) {
                    items.add(new DiagnosticItem("NETWORK", "Mobile Data", mobile.getState().name(), DiagnosticItem.Status.DETECTED));
                    Log.i(TAG, "mobileNetwork: state=" + mobile.getState());
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "ConnectivityManager error: " + t.getMessage(), t);
            items.add(new DiagnosticItem("NETWORK", "Connectivity", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
        }

        // 2. Wi-Fi: Differentiate between Radio (Client), Hotspot (AP Mode), and Connection
        collectWifiStatus(context, items);

        // 3. Hotspot Connected Clients Count (via dnsmasq.leases and bridge ARP validation)
        collectHotspotClients(items);

        // 4. Mobile Data Usage (rx_bytes / tx_bytes for cellular)
        collectMobileDataUsage(items);

        // 5. Network Interfaces & Addresses (rmnet_data0, bridge1, wlan0, rndis0)
        collectNetworkInterfaces(items);

        // 6. Default Gateway
        readDefaultGateway(items);
    }

    private void collectWifiStatus(Context context, List<DiagnosticItem> items) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                boolean wifiClientEnabled = wm.isWifiEnabled();
                int wifiState = wm.getWifiState();

                // Check Hotspot / AP Mode state via reflection (standard Android hidden API)
                int apState = -1;
                boolean isApEnabled = false;
                try {
                    Method getWifiApState = wm.getClass().getMethod("getWifiApState");
                    Object res = getWifiApState.invoke(wm);
                    if (res instanceof Integer) {
                        apState = (Integer) res;
                        // 13 = WIFI_AP_STATE_ENABLED, 12 = ENABLING
                        isApEnabled = (apState == 13 || apState == 12);
                    }
                } catch (Throwable t) {
                    Log.w(TAG_WIFI, "getWifiApState reflection not available: " + t.getMessage());
                }

                // Wi-Fi Radio summary
                String radioSummary;
                if (isApEnabled) {
                    radioSummary = "AP / Hotspot ON";
                } else if (wifiClientEnabled) {
                    radioSummary = "Client ON";
                } else {
                    radioSummary = "Disabled";
                }

                items.add(new DiagnosticItem("WIFI", "Radio Mode", radioSummary, DiagnosticItem.Status.DETECTED));
                items.add(new DiagnosticItem("WIFI", "Client Enabled", String.valueOf(wifiClientEnabled), DiagnosticItem.Status.DETECTED));
                items.add(new DiagnosticItem("WIFI", "Hotspot Enabled", String.valueOf(isApEnabled), DiagnosticItem.Status.DETECTED));

                Log.i(TAG_WIFI, "radioEnabled=" + wifiClientEnabled + " apEnabled=" + isApEnabled + " apState=" + apState + " wifiState=" + wifiState);

                // Extract Hotspot SSID and Password via WifiConfiguration reflection
                String apSsid = null;
                String apPass = null;
                try {
                    Method getWifiApConfiguration = wm.getClass().getMethod("getWifiApConfiguration");
                    Object apConfig = getWifiApConfiguration.invoke(wm);
                    if (apConfig != null) {
                        Class<?> configClass = apConfig.getClass();
                        try {
                            Object ssidObj = configClass.getField("SSID").get(apConfig);
                            if (ssidObj != null) apSsid = ssidObj.toString();
                        } catch (Throwable ignored) {}
                        try {
                            Object preSharedKeyObj = configClass.getField("preSharedKey").get(apConfig);
                            if (preSharedKeyObj != null) apPass = preSharedKeyObj.toString();
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable t) {
                    Log.w(TAG_WIFI, "getWifiApConfiguration reflection error: " + t.getMessage());
                }

                // Clean quotes if present (e.g. "SSID" -> SSID)
                if (apSsid != null) {
                    if (apSsid.startsWith("\"") && apSsid.endsWith("\"") && apSsid.length() >= 2) {
                        apSsid = apSsid.substring(1, apSsid.length() - 1);
                    }
                }
                if (apPass != null) {
                    if (apPass.startsWith("\"") && apPass.endsWith("\"") && apPass.length() >= 2) {
                        apPass = apPass.substring(1, apPass.length() - 1);
                    }
                }

                // If null or unavailable via reflection, query himiapi fallback
                if (apSsid == null || apPass == null) {
                    try {
                        String[] creds = queryHimiWifiCredentials();
                        if (creds != null) {
                            if (apSsid == null && creds[0] != null) apSsid = creds[0];
                            if (apPass == null && creds[1] != null) apPass = creds[1];
                        }
                    } catch (Throwable ignored) {}
                }

                if (apSsid != null && !apSsid.isEmpty()) {
                    items.add(new DiagnosticItem("WIFI", "Hotspot SSID", apSsid, DiagnosticItem.Status.DETECTED));
                }
                if (apPass != null && !apPass.isEmpty()) {
                    items.add(new DiagnosticItem("WIFI", "Hotspot Password", apPass, DiagnosticItem.Status.DETECTED));
                }

                if (wifiClientEnabled) {
                    WifiInfo winfo = wm.getConnectionInfo();
                    if (winfo != null && winfo.getSSID() != null && !winfo.getSSID().contains("unknown")) {
                        items.add(new DiagnosticItem("WIFI", "SSID", winfo.getSSID(), DiagnosticItem.Status.DETECTED));
                    }
                }
            } else {
                Log.w(TAG_WIFI, "WifiManager unavailable");
                items.add(new DiagnosticItem("WIFI", "Radio Mode", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            Log.e(TAG_WIFI, "Error inspecting WiFi: " + t.getMessage(), t);
            items.add(new DiagnosticItem("WIFI", "Radio Mode", "Error", DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    /**
     * Inspects active Wi-Fi wireless clients on hotspot.
     * Distinguishes USB RNDIS host (rndis0, port 2) from Wi-Fi wireless clients (wlan0, port 1).
     */
    private void collectHotspotClients(List<DiagnosticItem> items) {
        int wifiClientCount = 0;
        int totalDhcpClients = 0;

        // 1. Identify which MAC addresses are associated specifically on the Wi-Fi port (wlan0) of bridge1
        // Port 1 = wlan0 (Wi-Fi), Port 2 = rndis0 (USB Tethering)
        Set<String> wifiMacs = getBridgePortMacs("1");

        // 2. Read ARP table for reachable entries on bridge1
        File arpFile = new File("/proc/net/arp");
        Set<String> arpIps = new HashSet<String>();
        Set<String> arpMacs = new HashSet<String>();
        if (arpFile.exists() && arpFile.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(arpFile));
                String line = reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 6) {
                        String ip = tokens[0];
                        String flags = tokens[2];
                        String mac = tokens[3].toUpperCase(Locale.US);
                        String dev = tokens[5];
                        if ("bridge1".equals(dev) && !"0x0".equals(flags) && !"00:00:00:00:00:00".equals(mac)) {
                            arpIps.add(ip);
                            arpMacs.add(mac);
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (reader != null) try { reader.close(); } catch (Throwable ignored) {}
            }
        }

        // 3. Read dnsmasq.leases
        File leasesFile = new File("/data/misc/dhcp/dnsmasq.leases");
        List<HotspotClient> clientList = new java.util.ArrayList<HotspotClient>();
        if (leasesFile.exists() && leasesFile.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(leasesFile));
                String line;
                long now = System.currentTimeMillis() / 1000;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 4) {
                        try {
                            long expires = Long.parseLong(tokens[0]);
                            if (expires > now || expires == 0) { // Valid non-expired lease
                                String mac = tokens[1].toUpperCase(Locale.US);
                                String ip = tokens[2];
                                String hostname = tokens[3];
                                if ("*".equals(hostname) || hostname.isEmpty()) {
                                    hostname = "UNKNOWN";
                                }
                                totalDhcpClients++;

                                // CRITICAL: Exclude USB host (rndis0) and only count genuine Wi-Fi clients
                                boolean isWifiClient = false;
                                if (!wifiMacs.isEmpty()) {
                                    // If we can determine bridge ports, MAC must be on wlan0 (port 1)
                                    isWifiClient = wifiMacs.contains(mac);
                                } else {
                                    // Fallback: Must be in ARP and not match USB RNDIS local/host MAC
                                    isWifiClient = arpIps.contains(ip) && !isUsbHostMac(mac);
                                }

                                if (isWifiClient) {
                                    wifiClientCount++;
                                    clientList.add(new HotspotClient(hostname, ip, mac));
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG_WIFI, "Error reading dnsmasq.leases: " + t.getMessage());
            } finally {
                if (reader != null) try { reader.close(); } catch (Throwable ignored) {}
            }
        } else {
            Log.w(TAG_WIFI, "/data/misc/dhcp/dnsmasq.leases unreadable or not present");
        }

        sConnectedClients = clientList;

        items.add(new DiagnosticItem("WIFI", "Hotspot Clients", String.valueOf(wifiClientCount), DiagnosticItem.Status.DETECTED));
        items.add(new DiagnosticItem("WIFI", "Total Leases", String.valueOf(totalDhcpClients), DiagnosticItem.Status.DETECTED));
        Log.i(TAG_WIFI, "hotspotClients=" + wifiClientCount + " (wifiMacs=" + wifiMacs.size() + ") totalLeases=" + totalDhcpClients);
    }

    /**
     * Queries bridge1 forwarding database using brctl to identify active MACs on a specific port.
     * Port 1 corresponds to wlan0 (Wi-Fi), Port 2 corresponds to rndis0 (USB).
     */
    private Set<String> getBridgePortMacs(String targetPort) {
        Set<String> macs = new HashSet<String>();
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"brctl", "showmacs", "bridge1"});
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                // Expected format: port_no, mac_addr, is_local?, ageing_timer
                if (tokens.length >= 4) {
                    String port = tokens[0];
                    String mac = tokens[1].toUpperCase(Locale.US);
                    String isLocal = tokens[2];
                    if (targetPort.equals(port) && "no".equalsIgnoreCase(isLocal)) {
                        macs.add(mac);
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG_WIFI, "brctl query not possible: " + t.getMessage());
        } finally {
            if (reader != null) try { reader.close(); } catch (Throwable ignored) {}
            if (process != null) {
                try { process.destroy(); } catch (Throwable ignored) {}
            }
        }
        return macs;
    }

    private boolean isUsbHostMac(String mac) {
        // Known RNDIS host signatures or USB interfaces
        return mac.startsWith("02:53:56") || mac.startsWith("CE:9C:5D");
    }

    private static volatile List<HotspotClient> sConnectedClients = new java.util.ArrayList<HotspotClient>();

    public static List<HotspotClient> getConnectedClients() {
        return sConnectedClients;
    }

    public static class HotspotClient {
        private final String hostname;
        private final String ip;
        private final String mac;

        public HotspotClient(String hostname, String ip, String mac) {
            this.hostname = hostname;
            this.ip = ip;
            this.mac = mac;
        }

        public String getHostname() { return hostname; }
        public String getIp() { return ip; }
        public String getMac() { return mac; }
    }

    /**
     * Reads cellular mobile data usage directly from rmnet_data0 / rmnet0 sysfs statistics
     * and validates with TrafficStats.getMobileRxBytes() / getMobileTxBytes().
     */
    private void collectMobileDataUsage(List<DiagnosticItem> items) {
        long rxBytes = -1;
        long txBytes = -1;

        // Try direct sysfs statistics on cellular interface
        File rxFile = new File("/sys/class/net/rmnet_data0/statistics/rx_bytes");
        File txFile = new File("/sys/class/net/rmnet_data0/statistics/tx_bytes");
        if (!rxFile.exists()) {
            rxFile = new File("/sys/class/net/rmnet0/statistics/rx_bytes");
            txFile = new File("/sys/class/net/rmnet0/statistics/tx_bytes");
        }

        if (rxFile.exists() && rxFile.canRead() && txFile.exists() && txFile.canRead()) {
            BufferedReader rxReader = null;
            BufferedReader txReader = null;
            try {
                rxReader = new BufferedReader(new FileReader(rxFile));
                txReader = new BufferedReader(new FileReader(txFile));
                String rxStr = rxReader.readLine();
                String txStr = txReader.readLine();
                if (rxStr != null && txStr != null) {
                    rxBytes = Long.parseLong(rxStr.trim());
                    txBytes = Long.parseLong(txStr.trim());
                }
            } catch (Throwable t) {
                Log.w(TAG, "Error reading rmnet statistics: " + t.getMessage());
            } finally {
                if (rxReader != null) try { rxReader.close(); } catch (Throwable ignored) {}
                if (txReader != null) try { txReader.close(); } catch (Throwable ignored) {}
            }
        }

        // Fallback to TrafficStats if sysfs unreadable
        if (rxBytes < 0) {
            long tsRx = TrafficStats.getMobileRxBytes();
            long tsTx = TrafficStats.getMobileTxBytes();
            if (tsRx != TrafficStats.UNSUPPORTED && tsTx != TrafficStats.UNSUPPORTED) {
                rxBytes = tsRx;
                txBytes = tsTx;
            }
        }

        if (rxBytes >= 0 && txBytes >= 0) {
            String rxFormatted = formatBytes(rxBytes);
            String txFormatted = formatBytes(txBytes);
            String totalFormatted = formatBytes(rxBytes + txBytes);

            items.add(new DiagnosticItem("DATA", "Cellular DL", rxFormatted, DiagnosticItem.Status.DETECTED));
            items.add(new DiagnosticItem("DATA", "Cellular UL", txFormatted, DiagnosticItem.Status.DETECTED));
            items.add(new DiagnosticItem("DATA", "Cellular Total", totalFormatted, DiagnosticItem.Status.DETECTED));
            Log.i(TAG, "mobileData: rx=" + rxFormatted + " tx=" + txFormatted + " total=" + totalFormatted + " (session since boot)");
        } else {
            items.add(new DiagnosticItem("DATA", "Cellular DL", "N/A", DiagnosticItem.Status.UNAVAILABLE));
            items.add(new DiagnosticItem("DATA", "Cellular UL", "N/A", DiagnosticItem.Status.UNAVAILABLE));
            items.add(new DiagnosticItem("DATA", "Cellular Total", "N/A", DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    private void collectNetworkInterfaces(List<DiagnosticItem> items) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                Log.w(TAG, "NetworkInterface.getNetworkInterfaces() returned null");
                items.add(new DiagnosticItem("NETWORK", "Interfaces", "None", DiagnosticItem.Status.NOT_DETECTED));
                return;
            }

            int activeCount = 0;
            for (NetworkInterface nif : Collections.list(interfaces)) {
                String name = nif.getName();
                boolean isUp = nif.isUp();
                boolean isLoopback = nif.isLoopback();

                StringBuilder ipv4List = new StringBuilder();
                StringBuilder ipv6List = new StringBuilder();

                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String host = addr.getHostAddress();
                    if (host == null) continue;
                    int pct = host.indexOf('%');
                    if (pct > 0) host = host.substring(0, pct);

                    if (host.contains(":")) {
                        if (ipv6List.length() > 0) ipv6List.append(", ");
                        ipv6List.append(host);
                    } else {
                        if (ipv4List.length() > 0) ipv4List.append(", ");
                        ipv4List.append(host);
                    }
                }

                String displayVal;
                if (ipv4List.length() > 0) {
                    displayVal = ipv4List.toString();
                } else if (ipv6List.length() > 0) {
                    displayVal = "IPv6 Only (" + ipv6List.toString() + ")";
                } else if (isUp) {
                    displayVal = "UP (No IP)";
                } else {
                    displayVal = "DOWN";
                }

                if (isUp) activeCount++;

                // Record interface items
                items.add(new DiagnosticItem("INTERFACES", name, displayVal, isUp ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.REPORTED));

                Log.i(TAG, "Interface=" + name + " up=" + isUp + " loopback=" + isLoopback + " ipv4=" + (ipv4List.length() > 0 ? ipv4List.toString() : "none") + " ipv6=" + (ipv6List.length() > 0 ? ipv6List.toString() : "none"));
            }

            items.add(new DiagnosticItem("NETWORK", "Active Ifaces", String.valueOf(activeCount), DiagnosticItem.Status.DETECTED));
        } catch (Throwable t) {
            Log.e(TAG, "Error enumerating network interfaces: " + t.getMessage(), t);
            items.add(new DiagnosticItem("NETWORK", "Interfaces", "Error: " + t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void readDefaultGateway(List<DiagnosticItem> items) {
        File routeFile = new File("/proc/net/route");
        if (!routeFile.exists() || !routeFile.canRead()) {
            Log.w(TAG, "/proc/net/route not accessible");
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(routeFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 3 && "00000000".equals(tokens[1])) { // Default route
                    String iface = tokens[0];
                    String gwHex = tokens[2];
                    long gwInt = Long.parseLong(gwHex, 16);
                    String gatewayIp = String.format("%d.%d.%d.%d",
                            (gwInt & 0xff),
                            (gwInt >> 8 & 0xff),
                            (gwInt >> 16 & 0xff),
                            (gwInt >> 24 & 0xff));
                    items.add(new DiagnosticItem("NETWORK", "Default GW", gatewayIp + " (" + iface + ")", DiagnosticItem.Status.DETECTED));
                    Log.i(TAG, "DefaultRoute: gw=" + gatewayIp + " iface=" + iface);
                    break;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error reading /proc/net/route: " + t.getMessage(), t);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Throwable ignored) {}
            }
        }
    }
    private static String cachedApSsid = null;
    private static String cachedApPass = null;
    private static long lastHimiQueryTime = 0;

    /**
     * Fallback to local TravelFi himiapi to retrieve hotspot SSID and Password.
     */
    private static synchronized String[] queryHimiWifiCredentials() {
        long now = System.currentTimeMillis();
        if (cachedApSsid != null && cachedApPass != null && (now - lastHimiQueryTime < 60000)) {
            return new String[] { cachedApSsid, cachedApPass };
        }

        try {
            // 1. Login to himiwebserver
            java.net.URL loginUrl = new java.net.URL("http://192.168.100.1:8000/himiapi/json");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) loginUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setDoOutput(true);

            byte[] loginPayload = "{\"cmdid\":\"login\",\"username\":\"admin\",\"password\":\"admin\"}".getBytes("UTF-8");
            conn.getOutputStream().write(loginPayload);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            conn.disconnect();

            String loginResp = sb.toString();
            String session = null;
            int sessIdx = loginResp.indexOf("\"session\":");
            if (sessIdx != -1) {
                int start = loginResp.indexOf('"', sessIdx + 10);
                if (start != -1) {
                    int end = loginResp.indexOf('"', start + 1);
                    if (end != -1) {
                        session = loginResp.substring(start + 1, end);
                    }
                }
            }

            if (session == null || session.isEmpty()) {
                return null;
            }

            // 2. Query getwifiinfo with Authorization header
            java.net.HttpURLConnection wifiConn = (java.net.HttpURLConnection) loginUrl.openConnection();
            wifiConn.setRequestMethod("POST");
            wifiConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            wifiConn.setRequestProperty("Authorization", session);
            wifiConn.setConnectTimeout(2000);
            wifiConn.setReadTimeout(2000);
            wifiConn.setDoOutput(true);

            String wifiPayloadStr = "{\"cmdid\":\"getwifiinfo\",\"sessionId\":\"" + session + "\"}";
            wifiConn.getOutputStream().write(wifiPayloadStr.getBytes("UTF-8"));

            BufferedReader wifiReader = new BufferedReader(new InputStreamReader(wifiConn.getInputStream(), "UTF-8"));
            StringBuilder wifiSb = new StringBuilder();
            while ((line = wifiReader.readLine()) != null) {
                wifiSb.append(line);
            }
            wifiReader.close();
            wifiConn.disconnect();

            String wifiResp = wifiSb.toString();
            // Parse wifissid & wifipass
            int ssidIdx = wifiResp.indexOf("\"wifissid\":");
            if (ssidIdx != -1) {
                int sStart = wifiResp.indexOf('"', ssidIdx + 11);
                if (sStart != -1) {
                    int sEnd = wifiResp.indexOf('"', sStart + 1);
                    if (sEnd != -1) {
                        cachedApSsid = wifiResp.substring(sStart + 1, sEnd);
                    }
                }
            }

            int passIdx = wifiResp.indexOf("\"wifipass\":");
            if (passIdx != -1) {
                int pStart = wifiResp.indexOf('"', passIdx + 11);
                if (pStart != -1) {
                    int pEnd = wifiResp.indexOf('"', pStart + 1);
                    if (pEnd != -1) {
                        cachedApPass = wifiResp.substring(pStart + 1, pEnd);
                    }
                }
            }

            lastHimiQueryTime = now;
            if (cachedApSsid != null || cachedApPass != null) {
                return new String[] { cachedApSsid, cachedApPass };
            }
        } catch (Throwable t) {
            Log.w(TAG_WIFI, "queryHimiWifiCredentials error: " + t.getMessage());
        }
        return null;
    }
}
