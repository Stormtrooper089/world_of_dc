package org.dcoffice.cachar.entity;

public enum WasteUrgency {
    NORMAL("Normal", 48),
    URGENT("Urgent", 24),
    PUBLIC_HEALTH_RISK("Public health risk", 6);

    private final String displayName;
    private final int slaHours;

    WasteUrgency(String displayName, int slaHours) {
        this.displayName = displayName;
        this.slaHours = slaHours;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
