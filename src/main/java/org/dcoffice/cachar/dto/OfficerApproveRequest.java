package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.OfficerRole;

public class OfficerApproveRequest {
    private String approverEmployeeId;
    private OfficerRole role;

    public String getApproverEmployeeId() { return approverEmployeeId; }
    public void setApproverEmployeeId(String approverEmployeeId) { this.approverEmployeeId = approverEmployeeId; }

    public OfficerRole getRole() { return role; }
    public void setRole(OfficerRole role) { this.role = role; }
}
