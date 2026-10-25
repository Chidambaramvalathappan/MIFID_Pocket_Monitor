package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiagnosticRepository {

    private final List<InfoCollector> collectors = new ArrayList<InfoCollector>();
    private final List<DiagnosticItem> cachedItems = new ArrayList<DiagnosticItem>();

    public DiagnosticRepository() {
        collectors.add(new DeviceInfoCollector());
        collectors.add(new CpuMemoryCollector());
        collectors.add(new DisplayInfoCollector());
        collectors.add(new NetworkInfoCollector());
        collectors.add(new TelephonyInfoCollector());
        collectors.add(new HardwareCapabilityCollector());
    }

    public synchronized void refresh(Context context) {
        List<DiagnosticItem> freshItems = new ArrayList<DiagnosticItem>();
        for (InfoCollector c : collectors) {
            try {
                c.collect(context, freshItems);
            } catch (Throwable ignored) {}
        }
        cachedItems.clear();
        cachedItems.addAll(freshItems);
    }

    public synchronized List<DiagnosticItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<DiagnosticItem>(cachedItems));
    }

    public synchronized DiagnosticItem findItem(String category, String label) {
        for (DiagnosticItem item : cachedItems) {
            if (item.getCategory().equalsIgnoreCase(category) && item.getLabel().equalsIgnoreCase(label)) {
                return item;
            }
        }
        return null;
    }
}
