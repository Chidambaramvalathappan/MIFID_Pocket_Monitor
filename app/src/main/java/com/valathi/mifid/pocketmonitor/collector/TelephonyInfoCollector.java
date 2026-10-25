package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class TelephonyInfoCollector implements InfoCollector {

    private static final String TAG = "MIFID-CELL";

    private static volatile String lastSignalStr = "Checking...";
    private static volatile String lastSignalBars = "--";
    private static volatile int lastSignalLevel = -1; // 0..4, or -1 unknown
    private static volatile DiagnosticItem.Status lastSignalStatus = DiagnosticItem.Status.NOT_VERIFIED;
    private static PhoneStateListener sListener = null;

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                // Ensure PhoneStateListener is registered
                registerListenerIfNeeded(context, tm);

                // Operator Name
                String opName = tm.getNetworkOperatorName();
                if (opName != null && !opName.trim().isEmpty()) {
                    items.add(new DiagnosticItem("CELLULAR", "Operator", opName, DiagnosticItem.Status.DETECTED));
                } else {
                    String simOpName = tm.getSimOperatorName();
                    if (simOpName != null && !simOpName.trim().isEmpty()) {
                        items.add(new DiagnosticItem("CELLULAR", "Operator", simOpName, DiagnosticItem.Status.REPORTED));
                    } else {
                        items.add(new DiagnosticItem("CELLULAR", "Operator", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
                    }
                }

                // Operator Numeric Code
                String opCode = tm.getNetworkOperator();
                if (opCode != null && !opCode.trim().isEmpty()) {
                    items.add(new DiagnosticItem("CELLULAR", "Operator Code", opCode, DiagnosticItem.Status.DETECTED));
                }

                // Radio Network Type
                int netType = tm.getNetworkType();
                String netTypeName = getNetworkTypeName(netType);
                items.add(new DiagnosticItem("CELLULAR", "Network Type", netTypeName, DiagnosticItem.Status.DETECTED));

                // Phone Type
                int phoneType = tm.getPhoneType();
                items.add(new DiagnosticItem("CELLULAR", "Phone Type", getPhoneTypeName(phoneType), DiagnosticItem.Status.REPORTED));

                // Roaming state (crucial for travel)
                boolean isRoaming = tm.isNetworkRoaming();
                items.add(new DiagnosticItem("CELLULAR", "Roaming", isRoaming ? "ROAMING ON" : "HOME", DiagnosticItem.Status.DETECTED));

                // SIM State
                int simState = tm.getSimState();
                items.add(new DiagnosticItem("CELLULAR", "SIM State", getSimStateName(simState), DiagnosticItem.Status.DETECTED));

                // Data State
                int dataState = tm.getDataState();
                String dataStateName = getDataStateName(dataState);
                items.add(new DiagnosticItem("CELLULAR", "Data State", dataStateName, DiagnosticItem.Status.DETECTED));

                // Signal Strength & Bars
                items.add(new DiagnosticItem("CELLULAR", "Signal Strength", lastSignalStr, lastSignalStatus));
                items.add(new DiagnosticItem("CELLULAR", "Signal Bars", lastSignalBars, lastSignalStatus));
                items.add(new DiagnosticItem("CELLULAR", "Signal Level", String.valueOf(lastSignalLevel), lastSignalStatus));

                // Dynamic Cellular Identity Information (BS ID / TAC, Cell ID, IMEI, ICCID, IMSI)
                collectCellularIdentity(tm, items);

                Log.i(TAG, "operator=" + opName + " opCode=" + opCode + " networkType=" + netTypeName + " dataState=" + dataStateName + " signal=" + lastSignalStr + " bars=" + lastSignalBars);
            } else {
                Log.w(TAG, "TelephonyManager unavailable");
                items.add(new DiagnosticItem("CELLULAR", "TelephonyManager", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error collecting telephony info: " + t.getMessage(), t);
            items.add(new DiagnosticItem("CELLULAR", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private static String getSystemProperty(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", String.class);
            return (String) method.invoke(null, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private synchronized void registerListenerIfNeeded(Context context, final TelephonyManager tm) {
        if (sListener == null) {
            try {
                sListener = new PhoneStateListener() {
                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        super.onSignalStrengthsChanged(signalStrength);
                        parseSignalStrength(signalStrength);
                    }
                };
                tm.listen(sListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                Log.i(TAG, "PhoneStateListener registered for LISTEN_SIGNAL_STRENGTHS");
            } catch (Throwable t) {
                Log.w(TAG, "Failed to register PhoneStateListener: " + t.getMessage());
                lastSignalStr = "Permission/API Denied";
                lastSignalBars = "--";
                lastSignalLevel = -1;
                lastSignalStatus = DiagnosticItem.Status.UNAVAILABLE;
            }
        }
    }

    private static void parseSignalStrength(SignalStrength ss) {
        if (ss == null) {
            lastSignalStr = "Unavailable";
            lastSignalBars = "--";
            lastSignalLevel = -1;
            lastSignalStatus = DiagnosticItem.Status.UNAVAILABLE;
            return;
        }

        try {
            // Check LTE signal via reflection (LTE methods introduced in API 11/17)
            Integer lteDbm = null;
            Integer lteRsrp = null;
            try {
                Method getLteDbm = SignalStrength.class.getMethod("getLteDbm");
                Object res = getLteDbm.invoke(ss);
                if (res instanceof Integer && (Integer) res != -1 && (Integer) res != Integer.MAX_VALUE) {
                    lteDbm = (Integer) res;
                }
            } catch (Throwable ignored) {}

            try {
                Method getLteRsrp = SignalStrength.class.getMethod("getLteRsrp");
                Object res = getLteRsrp.invoke(ss);
                if (res instanceof Integer && (Integer) res != -1 && (Integer) res != Integer.MAX_VALUE && (Integer) res != 0) {
                    lteRsrp = (Integer) res;
                }
            } catch (Throwable ignored) {}

            if (lteRsrp != null) {
                lastSignalStr = lteRsrp + " dBm (RSRP)";
                lastSignalLevel = calculateLteLevel(lteRsrp);
                lastSignalBars = formatSignalBars(lastSignalLevel);
                lastSignalStatus = DiagnosticItem.Status.DETECTED;
                Log.i(TAG, "signalSource=LTE_RSRP value=" + lteRsrp + " unit=dBm bars=" + lastSignalBars);
                return;
            } else if (lteDbm != null) {
                lastSignalStr = lteDbm + " dBm (LTE)";
                lastSignalLevel = calculateLteLevel(lteDbm);
                lastSignalBars = formatSignalBars(lastSignalLevel);
                lastSignalStatus = DiagnosticItem.Status.DETECTED;
                Log.i(TAG, "signalSource=LTE_DBM value=" + lteDbm + " unit=dBm bars=" + lastSignalBars);
                return;
            }

            // Fallback to GSM ASU
            int gsmAsu = ss.getGsmSignalStrength();
            if (gsmAsu != 99 && gsmAsu >= 0 && gsmAsu <= 31) {
                int dbm = -113 + (2 * gsmAsu);
                lastSignalStr = dbm + " dBm (ASU " + gsmAsu + ")";
                lastSignalLevel = calculateGsmLevel(gsmAsu);
                lastSignalBars = formatSignalBars(lastSignalLevel);
                lastSignalStatus = DiagnosticItem.Status.DETECTED;
                Log.i(TAG, "signalSource=GSM_ASU value=" + dbm + " unit=dBm asu=" + gsmAsu + " bars=" + lastSignalBars);
                return;
            }

            // Check toString() for Qualcomm MSM format: e.g., "SignalStrength: 99 0 -120 -160 -120 -1 -1 26 -96 -14 28 ..."
            String sStr = ss.toString();
            String[] parts = sStr.split("\\s+");
            if (parts.length >= 11) {
                try {
                    int val = Integer.parseInt(parts[9]);
                    if (val < -40 && val > -145) { // valid RSRP range
                        lastSignalStr = val + " dBm (LTE)";
                        lastSignalLevel = calculateLteLevel(val);
                        lastSignalBars = formatSignalBars(lastSignalLevel);
                        lastSignalStatus = DiagnosticItem.Status.DETECTED;
                        Log.i(TAG, "signalSource=Parsed_LTE value=" + val + " unit=dBm bars=" + lastSignalBars);
                        return;
                    }
                } catch (Throwable ignored) {}
            }

            lastSignalStr = "Unknown (Code " + gsmAsu + ")";
            lastSignalBars = "--";
            lastSignalLevel = -1;
            lastSignalStatus = DiagnosticItem.Status.REPORTED;
            Log.w(TAG, "signalSource=Unknown raw=" + sStr);
        } catch (Throwable t) {
            Log.e(TAG, "Error parsing signal strength: " + t.getMessage(), t);
            lastSignalStr = "Parse Error";
            lastSignalBars = "--";
            lastSignalLevel = -1;
            lastSignalStatus = DiagnosticItem.Status.UNAVAILABLE;
        }
    }

    private static int calculateLteLevel(int rsrp) {
        if (rsrp > -44) return 0; // invalid/unrealistic
        if (rsrp >= -95) return 4; // Excellent
        if (rsrp >= -105) return 3; // Good
        if (rsrp >= -115) return 2; // Fair
        if (rsrp >= -125) return 1; // Poor
        return 0; // None
    }

    private static int calculateGsmLevel(int asu) {
        if (asu <= 2 || asu == 99) return 0;
        if (asu >= 12) return 4;
        if (asu >= 8) return 3;
        if (asu >= 5) return 2;
        return 1;
    }

    private static String formatSignalBars(int level) {
        switch (level) {
            case 4: return "[||||]";
            case 3: return "[|||.]";
            case 2: return "[||..]";
            case 1: return "[|...]";
            case 0: return "[....]";
            default: return "--";
        }
    }

    public static String getNetworkTypeName(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "IDEN";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
            case TelephonyManager.NETWORK_TYPE_EHRPD: return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
            default: return "Unknown (" + type + ")";
        }
    }

    private String getPhoneTypeName(int type) {
        switch (type) {
            case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP: return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE: return "None";
            default: return "Unknown (" + type + ")";
        }
    }

    private String getSimStateName(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_READY: return "Ready";
            case TelephonyManager.SIM_STATE_ABSENT: return "Absent";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN Req";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK Req";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "Net Locked";
            case TelephonyManager.SIM_STATE_UNKNOWN: return "Unknown";
            default: return "Code " + state;
        }
    }

    private String getDataStateName(int state) {
        switch (state) {
            case TelephonyManager.DATA_CONNECTED: return "Connected";
            case TelephonyManager.DATA_CONNECTING: return "Connecting";
            case TelephonyManager.DATA_DISCONNECTED: return "Disconnected";
            case TelephonyManager.DATA_SUSPENDED: return "Suspended";
            default: return "Unknown (" + state + ")";
        }
    }

    private static String cachedImei = null;
    private static String cachedImsi = null;
    private static String cachedIccid = null;
    private static String cachedBsid = null;
    private static String cachedCellid = null;
    private static long lastHimiCellQuery = 0;

    /**
     * Dynamically collects cellular identity information:
     * - BS ID / TAC
     * - Cell ID
     * - IMEI
     * - ICCID
     * - IMSI
     * Note: Does not log sensitive identifiers (IMEI, IMSI, ICCID) to logcat.
     */
    private void collectCellularIdentity(TelephonyManager tm, List<DiagnosticItem> items) {
        String imei = null;
        String imsi = null;
        String iccid = null;
        String cellId = null;
        String bsId = null;

        // 1. Query Android TelephonyManager standard methods (READ_PHONE_STATE permission)
        try {
            imei = tm.getDeviceId();
        } catch (Throwable ignored) {}

        try {
            imsi = tm.getSubscriberId();
        } catch (Throwable ignored) {}

        try {
            iccid = tm.getSimSerialNumber();
        } catch (Throwable ignored) {}

        // 2. Query CellLocation (GSM / LTE CID & LAC/TAC)
        try {
            android.telephony.CellLocation loc = tm.getCellLocation();
            if (loc instanceof android.telephony.gsm.GsmCellLocation) {
                android.telephony.gsm.GsmCellLocation gsmLoc = (android.telephony.gsm.GsmCellLocation) loc;
                int cid = gsmLoc.getCid();
                int lac = gsmLoc.getLac();
                if (cid > 0 && cid != 0xffff && cid != Integer.MAX_VALUE) {
                    cellId = String.valueOf(cid);
                }
                if (lac > 0 && lac != 0xffff && lac != Integer.MAX_VALUE) {
                    bsId = String.valueOf(lac);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Fallback / Augment via local modem web API (himiapi getsysinfo & getoverview)
        long now = System.currentTimeMillis();
        if (cellId == null || bsId == null || imei == null || imsi == null || iccid == null) {
            if (now - lastHimiCellQuery > 10000 || cachedCellid == null) {
                queryHimiCellularIdentity();
            }
            if (cellId == null) cellId = cachedCellid;
            if (bsId == null) bsId = cachedBsid;
            if (imei == null) imei = cachedImei;
            if (imsi == null) imsi = cachedImsi;
            if (iccid == null) iccid = cachedIccid;
        }

        // Add items to CELLULAR category
        items.add(new DiagnosticItem("CELLULAR", "BS ID / TAC", (bsId != null && !bsId.isEmpty()) ? bsId : "N/A",
                bsId != null ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.UNAVAILABLE));

        items.add(new DiagnosticItem("CELLULAR", "Cell ID", (cellId != null && !cellId.isEmpty()) ? cellId : "N/A",
                cellId != null ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.UNAVAILABLE));

        items.add(new DiagnosticItem("CELLULAR", "IMEI", (imei != null && !imei.isEmpty()) ? imei : "N/A",
                imei != null ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.UNAVAILABLE));

        items.add(new DiagnosticItem("CELLULAR", "ICCID", (iccid != null && !iccid.isEmpty()) ? iccid : "N/A",
                iccid != null ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.UNAVAILABLE));

        items.add(new DiagnosticItem("CELLULAR", "IMSI", (imsi != null && !imsi.isEmpty()) ? imsi : "N/A",
                imsi != null ? DiagnosticItem.Status.DETECTED : DiagnosticItem.Status.UNAVAILABLE));
    }

    private static synchronized void queryHimiCellularIdentity() {
        try {
            java.net.URL loginUrl = new java.net.URL("http://192.168.100.1:8000/himiapi/json");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) loginUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setDoOutput(true);

            byte[] loginPayload = "{\"cmdid\":\"login\",\"username\":\"admin\",\"password\":\"admin\"}".getBytes("UTF-8");
            conn.getOutputStream().write(loginPayload);

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
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
                return;
            }

            // Query getsysinfo (bsid, cellid)
            java.net.HttpURLConnection sysConn = (java.net.HttpURLConnection) loginUrl.openConnection();
            sysConn.setRequestMethod("POST");
            sysConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            sysConn.setRequestProperty("Authorization", session);
            sysConn.setConnectTimeout(2000);
            sysConn.setReadTimeout(2000);
            sysConn.setDoOutput(true);

            String sysPayload = "{\"cmdid\":\"getsysinfo\",\"sessionId\":\"" + session + "\"}";
            sysConn.getOutputStream().write(sysPayload.getBytes("UTF-8"));

            java.io.BufferedReader sysReader = new java.io.BufferedReader(new java.io.InputStreamReader(sysConn.getInputStream(), "UTF-8"));
            StringBuilder sysSb = new StringBuilder();
            while ((line = sysReader.readLine()) != null) {
                sysSb.append(line);
            }
            sysReader.close();
            sysConn.disconnect();

            String sysResp = sysSb.toString();
            cachedBsid = extractJsonValue(sysResp, "bsid");
            cachedCellid = extractJsonValue(sysResp, "cellid");

            // Query getoverview (IMEI, IMSI, ICCID)
            java.net.HttpURLConnection ovConn = (java.net.HttpURLConnection) loginUrl.openConnection();
            ovConn.setRequestMethod("POST");
            ovConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            ovConn.setRequestProperty("Authorization", session);
            ovConn.setConnectTimeout(2000);
            ovConn.setReadTimeout(2000);
            ovConn.setDoOutput(true);

            String ovPayload = "{\"cmdid\":\"getoverview\",\"sessionId\":\"" + session + "\"}";
            ovConn.getOutputStream().write(ovPayload.getBytes("UTF-8"));

            java.io.BufferedReader ovReader = new java.io.BufferedReader(new java.io.InputStreamReader(ovConn.getInputStream(), "UTF-8"));
            StringBuilder ovSb = new StringBuilder();
            while ((line = ovReader.readLine()) != null) {
                ovSb.append(line);
            }
            ovReader.close();
            ovConn.disconnect();

            String ovResp = ovSb.toString();
            cachedImei = extractJsonValue(ovResp, "IMEI");
            cachedImsi = extractJsonValue(ovResp, "IMSI");
            cachedIccid = extractJsonValue(ovResp, "ICCID");

            lastHimiCellQuery = System.currentTimeMillis();
        } catch (Throwable t) {
            Log.w(TAG, "queryHimiCellularIdentity error: " + t.getClass().getName() + " : " + t.getMessage());
        }
    }

    private static String extractJsonValue(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx == -1) {
                idx = json.indexOf("\"" + key.toLowerCase(Locale.US) + "\":");
            }
            if (idx != -1) {
                int start = json.indexOf('"', idx + key.length() + 3);
                if (start != -1) {
                    int end = json.indexOf('"', start + 1);
                    if (end != -1) {
                        return json.substring(start + 1, end);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
