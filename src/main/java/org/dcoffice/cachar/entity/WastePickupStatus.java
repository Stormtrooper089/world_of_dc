package org.dcoffice.cachar.entity;

public enum WastePickupStatus {
    SUBMITTED("Submitted"),
    VERIFIED("Verified"),
    ASSIGNED("Assigned"),
    PICKUP_SCHEDULED("Pickup Scheduled"),
    IN_PROGRESS("In Progress"),
    PICKED_UP("Picked Up"),
    CLOSED("Closed"),
    REJECTED("Rejected"),
    REOPENED("Reopened");

    private final String displayName;

    WastePickupStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isClosedState() {
        return this == PICKED_UP || this == CLOSED || this == REJECTED;
    }
}
