package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.OfficerRole;

public class OfficerApproveRequest {
    private String approverEmployeeId;
    private String role; // Changed to String to accept from frontend

    public String getApproverEmployeeId() { return approverEmployeeId; }
    public void setApproverEmployeeId(String approverEmployeeId) { this.approverEmployeeId = approverEmployeeId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    // Helper method to convert string to enum
    public OfficerRole getRoleAsEnum() {
        if (role == null) return null;
        try {
            return OfficerRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return OfficerRole.OFFICER; // Default to OFFICER if invalid
        }
    }
}
