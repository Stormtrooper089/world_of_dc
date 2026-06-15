package org.dcoffice.cachar.entity;

public enum ComplaintCategory {
    GARBAGE_NOT_COLLECTED("Garbage Not Collected"),
    ILLEGAL_DUMPING("Illegal Dumping"),
    DRAIN_BLOCKAGE("Drain Blockage"),
    WATER_LOGGING("Water Logging"),
    STREET_LIGHT("Street Light"),
    ROAD_DAMAGE("Road Damage"),
    PUBLIC_TOILET("Public Toilet"),
    STRAY_ANIMAL("Stray Animal"),
    TRADE_LICENSE("Trade License"),
    PROPERTY_TAX("Property Tax"),
    BUILDING_PERMISSION("Building Permission"),
    WATER_SUPPLY("Water Supply"),
    ELECTRICITY("Electricity"),
    ROADS_INFRASTRUCTURE("Roads & Infrastructure"),
    HEALTH_SERVICES("Health Services"),
    EDUCATION("Education"),
    SANITATION("Sanitation"),
    PUBLIC_DISTRIBUTION_SYSTEM("Public Distribution System"),
    REVENUE_SERVICES("Revenue Services"),
    POLICE_SERVICES("Police Services"),
    CORRUPTION("Corruption"),
    ENVIRONMENTAL_ISSUES("Environmental Issues"),
    AGRICULTURE("Agriculture"),
    PENSION_SERVICES("Pension Services"),
    BIRTH_DEATH_CERTIFICATE("Birth/Death Certificate"),
    OTHER("Other");

    private final String displayName;

    ComplaintCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
