package org.dcoffice.cachar.entity;

public enum WasteCategory {
    HOUSEHOLD_WASTE_NOT_COLLECTED("Household waste not collected"),
    BULK_WASTE("Bulk waste"),
    CONSTRUCTION_DEMOLITION_WASTE("Construction and demolition waste"),
    DEAD_ANIMAL("Dead animal"),
    DRAIN_SILT_GARBAGE("Drain silt / garbage"),
    MARKET_WASTE("Market waste"),
    FESTIVAL_EVENT_WASTE("Festival/event waste"),
    OTHER("Other");

    private final String displayName;

    WasteCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
