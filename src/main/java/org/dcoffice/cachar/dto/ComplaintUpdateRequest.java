package org.dcoffice.cachar.dto;

import org.dcoffice.cachar.entity.ComplaintStatus;
import org.dcoffice.cachar.entity.Department;
import org.dcoffice.cachar.entity.Priority;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * DTO for updating complaint details
 * Only DC or complaint creator can modify complaints
 */
public class ComplaintUpdateRequest {

    @NotNull(message = "Complaint ID is required")
    private Long complaintId;

    private String subject;
    private String description;
    private String location;
    private Priority priority;
    private ComplaintStatus status;
    private Department assignedDepartment;
    private String departmentRemarks;
    private String assignedToId; // Officer ID to assign/reassign the complaint to

    // Getters and Setters
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public Department getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(Department assignedDepartment) { this.assignedDepartment = assignedDepartment; }

    public String getDepartmentRemarks() { return departmentRemarks; }
    public void setDepartmentRemarks(String departmentRemarks) { this.departmentRemarks = departmentRemarks; }


    public String getAssignedToId() { return assignedToId; }
    public void setAssignedToId(String assignedToId) { this.assignedToId = assignedToId; }
}

