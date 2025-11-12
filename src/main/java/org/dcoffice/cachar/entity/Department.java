package org.dcoffice.cachar.entity;

/**
 * Department enum for complaint assignment and management
 * 
 * @author District Cachar IT Team
 */
public enum Department {
    
    // Core Administrative Departments
    DISTRICT_ADMINISTRATION("District Administration", "Overall district administration"),
    REVENUE_DEPARTMENT("Revenue Department", "Land records, revenue collection, and property matters"),
    COLLECTORATE("Collectorate", "District collector's office"),
    
    // Infrastructure Departments
    PUBLIC_WORKS_DEPARTMENT("Public Works Department (PWD)", "Roads, bridges, and infrastructure development"),
    WATER_RESOURCES("Water Resources", "Water supply, irrigation, and water management"),
    ELECTRICITY_DEPARTMENT("Electricity Department", "Power supply and electrical infrastructure"),
    
    // Social Services
    HEALTH_DEPARTMENT("Health Department", "Healthcare services and medical facilities"),
    EDUCATION_DEPARTMENT("Education Department", "Schools, colleges, and educational services"),
    SOCIAL_WELFARE("Social Welfare", "Social security and welfare programs"),
    
    // Public Services
    POLICE_DEPARTMENT("Police Department", "Law enforcement and public safety"),
    FIRE_SERVICES("Fire Services", "Fire safety and emergency response"),
    TRANSPORT_DEPARTMENT("Transport Department", "Public transportation and vehicle registration"),
    
    // Development and Planning
    RURAL_DEVELOPMENT("Rural Development", "Rural infrastructure and development programs"),
    URBAN_DEVELOPMENT("Urban Development", "Urban planning and municipal services"),
    AGRICULTURE_DEPARTMENT("Agriculture Department", "Agricultural services and farmer support"),
    
    // Environmental and Utilities
    ENVIRONMENT_DEPARTMENT("Environment Department", "Environmental protection and conservation"),
    SANITATION_DEPARTMENT("Sanitation Department", "Waste management and sanitation services"),
    FOREST_DEPARTMENT("Forest Department", "Forest conservation and wildlife management"),
    
    // Specialized Services
    FOOD_AND_SUPPLIES("Food and Supplies", "Public distribution system and food security"),
    LABOUR_DEPARTMENT("Labour Department", "Labour welfare and employment services"),
    WOMEN_AND_CHILD_DEVELOPMENT("Women and Child Development", "Women and child welfare programs"),
    
    // Technical Departments
    INFORMATION_TECHNOLOGY("Information Technology", "IT services and digital governance"),
    STATISTICS_DEPARTMENT("Statistics Department", "Data collection and statistical services"),
    
    // Other
    OTHER("Other", "Other departments not specifically listed"),
    UNASSIGNED("Unassigned", "Not yet assigned to any department");
    
    private final String displayName;
    private final String description;
    
    Department(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

