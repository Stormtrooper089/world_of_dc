package org.dcoffice.cachar.entity;

public enum WasteQuantityEstimate {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    TRUCK_REQUIRED("Truck required");

    private final String displayName;

    WasteQuantityEstimate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
