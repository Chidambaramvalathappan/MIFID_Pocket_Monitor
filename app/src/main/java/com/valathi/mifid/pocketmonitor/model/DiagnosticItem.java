package com.valathi.mifid.pocketmonitor.model;

/**
 * Represents a single diagnostic or hardware inventory item.
 */
public class DiagnosticItem {

    public enum Status {
        DETECTED,
        REPORTED,
        UNAVAILABLE,
        NOT_DETECTED,
        NOT_VERIFIED
    }

    private final String category;
    private final String label;
    private final String value;
    private final Status status;

    public DiagnosticItem(String category, String label, String value, Status status) {
        this.category = category;
        this.label = label;
        this.value = value != null ? value : "N/A";
        this.status = status != null ? status : Status.NOT_VERIFIED;
    }

    public String getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return label + ": " + value + " [" + status.name() + "]";
    }
}
