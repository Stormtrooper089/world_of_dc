package org.dcoffice.cachar.entity;

/**
 * Comprehensive complaint status enum for tracking complaint lifecycle
 * 
 * @author District Cachar IT Team
 */
public enum ComplaintStatus {
    
    // Initial state
    CREATED("Created", "Complaint has been submitted and is awaiting review"),

    // Assignment state
    ASSIGNED("Assigned", "Complaint has been assigned to a department/officer"),

    // Active work state
    IN_PROGRESS("In Progress", "Complaint is being actively worked on"),

    // Blocked state
    BLOCKED("Blocked", "Complaint is blocked due to external dependencies"),

    // Resolution state
    RESOLVED("Resolved", "Complaint has been resolved"),

    // Final states
    CLOSED("Closed", "Complaint has been closed"),
    REJECTED("Rejected", "Complaint has been rejected as invalid"),
    DUPLICATE("Duplicate", "Complaint is a duplicate of existing complaint");
    
    private final String displayName;
    private final String description;
    
    ComplaintStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the status indicates the complaint is in active work
     */
    public boolean isActive() {
        return this == IN_PROGRESS || this == ASSIGNED;
    }
    
    /**
     * Check if the status indicates the complaint is blocked
     */
    public boolean isBlocked() {
        return this == BLOCKED;
    }
    
    /**
     * Check if the status indicates the complaint is resolved
     */
    public boolean isResolved() {
        return this == RESOLVED || this == CLOSED;
    }
    
    /**
     * Check if the status indicates the complaint is final (no further action needed)
     */
    public boolean isFinal() {
        return this == CLOSED || this == REJECTED || this == DUPLICATE;
    }

    // ----- State Machine -----
    private static final java.util.Map<ComplaintStatus, java.util.Set<ComplaintStatus>> ALLOWED_TRANSITIONS;
    static {
        java.util.Map<ComplaintStatus, java.util.Set<ComplaintStatus>> map = new java.util.EnumMap<>(ComplaintStatus.class);

        map.put(CREATED, java.util.EnumSet.of(ASSIGNED, REJECTED));

        map.put(ASSIGNED, java.util.EnumSet.of(IN_PROGRESS, BLOCKED));

        map.put(IN_PROGRESS, java.util.EnumSet.of(RESOLVED, BLOCKED));

        map.put(BLOCKED, java.util.EnumSet.of(IN_PROGRESS));

        map.put(RESOLVED, java.util.EnumSet.of(CLOSED));

        map.put(CLOSED, java.util.EnumSet.noneOf(ComplaintStatus.class));
        map.put(REJECTED, java.util.EnumSet.noneOf(ComplaintStatus.class));
        map.put(DUPLICATE, java.util.EnumSet.of(ASSIGNED));

        ALLOWED_TRANSITIONS = java.util.Collections.unmodifiableMap(map);
    }

    public java.util.Set<ComplaintStatus> allowedNextStatuses() {
        return ALLOWED_TRANSITIONS.getOrDefault(this, java.util.EnumSet.noneOf(ComplaintStatus.class));
    }

    public static boolean isValidTransition(ComplaintStatus from, ComplaintStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true; // idempotent
        return ALLOWED_TRANSITIONS.getOrDefault(from, java.util.EnumSet.noneOf(ComplaintStatus.class)).contains(to);
    }
}