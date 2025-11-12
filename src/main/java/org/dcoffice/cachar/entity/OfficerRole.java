// OfficerRole.java
package org.dcoffice.cachar.entity;

public enum OfficerRole {
    DISTRICT_COMMISSIONER("District Commissioner"),
    ADDITIONAL_DISTRICT_COMMISSIONER("Additional District Commissioner"),
    CIRCLE_OFFICER("Circle Officer"),
    BLOCK_DEVELOPMENT_OFFICER("Block Development Officer"),
    TEHSILDAR("Tehsildar"),
    SUB_DIVISIONAL_OFFICER("Sub Divisional Officer"),
    HEALTH_OFFICER("Health Officer"),
    EDUCATION_OFFICER("Education Officer"),
    PWD_OFFICER("PWD Officer"),
    POLICE_OFFICER("Police Officer"),
    OFFICER("Officer"),
    OTHER("Other");

    private final String displayName;

    OfficerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
