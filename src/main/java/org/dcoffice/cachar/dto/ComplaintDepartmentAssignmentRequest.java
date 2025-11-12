package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.Department;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * DTO for assigning complaints to departments
 * Only DC can assign departments
 */
public class ComplaintDepartmentAssignmentRequest {

    @NotNull(message = "Complaint ID is required")
    private Long complaintId;

    @NotNull(message = "Department is required")
    private Department department;

    @NotBlank(message = "Assignment remarks are required")
    private String assignmentRemarks;

    // Getters and Setters
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getAssignmentRemarks() { return assignmentRemarks; }
    public void setAssignmentRemarks(String assignmentRemarks) { this.assignmentRemarks = assignmentRemarks; }
}

