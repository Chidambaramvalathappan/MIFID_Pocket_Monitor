package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import android.os.Build;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.util.List;

public class DeviceInfoCollector implements InfoCollector {

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        try {
            items.add(new DiagnosticItem("DEVICE", "Model", Build.MODEL, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Manufacturer", Build.MANUFACTURER, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Brand", Build.BRAND, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Product", Build.PRODUCT, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Device", Build.DEVICE, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Board", Build.BOARD, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("DEVICE", "Hardware", Build.HARDWARE, DiagnosticItem.Status.REPORTED));

            items.add(new DiagnosticItem("SYSTEM", "Android Ver", Build.VERSION.RELEASE, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("SYSTEM", "SDK / API", String.valueOf(Build.VERSION.SDK_INT), DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("SYSTEM", "Build ID", Build.ID, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("SYSTEM", "Build Type", Build.TYPE, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("SYSTEM", "Fingerprint", Build.FINGERPRINT, DiagnosticItem.Status.REPORTED));
            items.add(new DiagnosticItem("SYSTEM", "Tags", Build.TAGS, DiagnosticItem.Status.REPORTED));
        } catch (Throwable t) {
            items.add(new DiagnosticItem("DEVICE", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }
}
