package com.valathi.mifid.pocketmonitor.collector;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.valathi.mifid.pocketmonitor.model.DiagnosticItem;
import java.util.List;

public class DisplayInfoCollector implements InfoCollector {

    @Override
    public void collect(Context context, List<DiagnosticItem> items) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);

                items.add(new DiagnosticItem("DISPLAY", "Resolution", metrics.widthPixels + "x" + metrics.heightPixels, DiagnosticItem.Status.DETECTED));
                items.add(new DiagnosticItem("DISPLAY", "Density DPI", String.valueOf(metrics.densityDpi), DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("DISPLAY", "Density Scale", String.valueOf(metrics.density), DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("DISPLAY", "Physical X-DPI", String.format("%.1f", metrics.xdpi), DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("DISPLAY", "Physical Y-DPI", String.format("%.1f", metrics.ydpi), DiagnosticItem.Status.REPORTED));
                items.add(new DiagnosticItem("DISPLAY", "Refresh Rate", String.format("%.1f Hz", display.getRefreshRate()), DiagnosticItem.Status.DETECTED));
            } else {
                items.add(new DiagnosticItem("DISPLAY", "WindowManager", "Unavailable", DiagnosticItem.Status.UNAVAILABLE));
            }
        } catch (Throwable t) {
            items.add(new DiagnosticItem("DISPLAY", "Error", t.getMessage(), DiagnosticItem.Status.UNAVAILABLE));
        }
    }
}
