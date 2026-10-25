package com.valathi.mifid.pocketmonitor.collector;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class CpuMemoryCollector implements InfoCollector {

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        // CPU
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            items.add(new DiagnosticItem("CPU", "Cores", String.valueOf(cores), DiagnosticItem.Status.DETECTED));
            items.add(new DiagnosticItem("CPU", "ABI", Build.CPU_ABI, DiagnosticItem.Status.REPORTED));
            if (Build.CPU_ABI2 != null && !Build.CPU_ABI2.isEmpty()) {
                items.add(new DiagnosticItem("CPU", "ABI2", Build.CPU_ABI2, DiagnosticItem.Status.REPORTED));
            }

            readCpuInfo(items);
        } catch (Throwable t) {
            items.add(new DiagnosticItem("CPU", "Status", "Error reading CPU", DiagnosticItem.Status.UNAVAILABLE));
        }

        // Memory
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);

                long availMb = mi.availMem / (1024 * 1024);
                items.add(new DiagnosticItem("MEMORY", "Avail RAM", availMb + " MB", DiagnosticItem.Status.DETECTED));
                items.add(new DiagnosticItem("MEMORY", "Low Memory", String.valueOf(mi.lowMemory), DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("MEMORY", "Mem Class", am.getMemoryClass() + " MB", DiagnosticItem.Status.REPORTED));

                if (Build.VERSION.SDK_INT >= 16) {
                    long totalMb = mi.totalMem / (1024 * 1024);
                    items.add(new DiagnosticItem("MEMORY", "Total RAM", totalMb + " MB", DiagnosticItem.Status.DETECTED));
                }
            }

            readMemInfo(items);
        } catch (Throwable t) {
            items.add(new DiagnosticItem("MEMORY", "Status", "Error reading RAM", DiagnosticItem.Status.UNAVAILABLE));
        }

        // Storage
        try {
            File internalData = Environment.getDataDirectory();
            if (internalData != null && internalData.exists()) {
                StatFs stat = new StatFs(internalData.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                long availBlocks = stat.getAvailableBlocks();

                long totalMb = (blockSize * totalBlocks) / (1024 * 1024);
                long availMb = (blockSize * availBlocks) / (1024 * 1024);
                items.add(new DiagnosticItem("STORAGE", "Internal Free", availMb + " / " + totalMb + " MB", DiagnosticItem.Status.DETECTED));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("STORAGE", "Internal", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
        }
    }

    private void readCpuInfo(List<DiagnosticItem> items) {
        File file = new File("/proc/cpuinfo");
        if (!file.exists() || !file.canRead()) {
            items.add(new DiagnosticItem("CPU", "ProcCpuinfo", "Unreadable", DiagnosticItem.Status.UNAVAILABLE));
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            String hardware = null;
            String processor = null;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    if ("Hardware".equalsIgnoreCase(key) && hardware == null) {
                        hardware = val;
                    } else if (("Processor".equalsIgnoreCase(key) || "model name".equalsIgnoreCase(key)) && processor == null) {
                        processor = val;
                    }
                }
            }

            if (hardware != null) {
                items.add(new DiagnosticItem("CPU", "Hardware", hardware, DiagnosticItem.Status.DETECTED));
            }
            if (processor != null) {
                items.add(new DiagnosticItem("CPU", "Model", processor, DiagnosticItem.Status.DETECTED));
            }

            // CPU Frequency (from /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq)
            File freqFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            if (freqFile.exists() && freqFile.canRead()) {
                BufferedReader fReader = null;
                try {
                    fReader = new BufferedReader(new FileReader(freqFile));
                    String fLine = fReader.readLine();
                    if (fLine != null) {
                        long khz = Long.parseLong(fLine.trim());
                        long mhz = khz / 1000;
                        items.add(new DiagnosticItem("CPU", "Frequency", mhz + " MHz", DiagnosticItem.Status.DETECTED));
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (fReader != null) try { fReader.close(); } catch (Throwable ignored) {}
                }
            }

            // Thermal Zone 0 (SoC Core Temp)
            File thermFile = new File("/sys/class/thermal/thermal_zone0/temp");
            if (thermFile.exists() && thermFile.canRead()) {
                BufferedReader tReader = null;
                try {
                    tReader = new BufferedReader(new FileReader(thermFile));
                    String tLine = tReader.readLine();
                    if (tLine != null) {
                        int raw = Integer.parseInt(tLine.trim());
                        int tempC = (raw > 1000) ? (raw / 1000) : raw;
                        items.add(new DiagnosticItem("CPU", "SoC Temp", tempC + "°C", DiagnosticItem.Status.DETECTED));
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (tReader != null) try { tReader.close(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private void readMemInfo(List<DiagnosticItem> items) {
        File file = new File("/proc/meminfo");
        if (!file.exists() || !file.canRead()) return;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    items.add(new DiagnosticItem("MEMORY", "Kernel Total", line.substring(9).trim(), DiagnosticItem.Status.DETECTED));
                } else if (line.startsWith("MemFree:")) {
                    items.add(new DiagnosticItem("MEMORY", "Kernel Free", line.substring(8).trim(), DiagnosticItem.Status.DETECTED));
                } else if (line.startsWith("SwapTotal:")) {
                    items.add(new DiagnosticItem("MEMORY", "Swap Total", line.substring(10).trim(), DiagnosticItem.Status.DETECTED));
                } else if (line.startsWith("SwapFree:")) {
                    items.add(new DiagnosticItem("MEMORY", "Swap Free", line.substring(9).trim(), DiagnosticItem.Status.DETECTED));
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {}
            }
        }
    }
}
