package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.util.List;

public interface InfoCollector {
    /**
     * Collects diagnostics safely without throwing exceptions or blocking indefinitely.
     */
    void collect(Context context, List<DiagnosticItem> items);
}
