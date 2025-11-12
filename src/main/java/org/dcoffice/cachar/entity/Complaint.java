package org.dcoffice.cachar.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "complaints")
public class Complaint {

    @Id
    private String id;



    @Indexed(unique = true)
    private Long complaintId;

    @Indexed(unique = true)
    private String complaintNumber;

    // Store citizen ID instead of reference
    @Indexed
    private String citizenId;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;

    private Priority priority = Priority.MEDIUM;

    @Indexed
    private ComplaintStatus status = ComplaintStatus.CREATED;

    private String location;

    // Department assignment
    private Department assignedDepartment = Department.UNASSIGNED;
    private String departmentRemarks;

    // Store officer IDs instead of references
    @Indexed
    @NotBlank(message = "Assigned officer is required")
    private String assignedToId;
    private String assignedById;
    @NotBlank(message = "Created by officer is required")
    private String createdById; // Track who created the complaint

    private String assignmentRemarks;
    private LocalDateTime assignedAt;

    // Comments - embedded for better performance (will be populated from comments collection)
    private List<Comment> comments;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    // Embedded documents for better performance
    private List<ComplaintDocument> documents;
    private List<ComplaintHistory> history;

    public Complaint() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status == ComplaintStatus.CLOSED || status == ComplaintStatus.RESOLVED) {
            if (this.closedAt == null) {
                this.closedAt = LocalDateTime.now();
            }
        }
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAssignedToId() { return assignedToId; }
    public void setAssignedToId(String assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedById() { return assignedById; }
    public void setAssignedById(String assignedById) { this.assignedById = assignedById; }

    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }

    public String getAssignmentRemarks() { return assignmentRemarks; }
    public void setAssignmentRemarks(String assignmentRemarks) { this.assignmentRemarks = assignmentRemarks; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public List<ComplaintDocument> getDocuments() { return documents; }
    public void setDocuments(List<ComplaintDocument> documents) { this.documents = documents; }

    public List<ComplaintHistory> getHistory() { return history; }
    public void setHistory(List<ComplaintHistory> history) { this.history = history; }

    // Department getters and setters
    public Department getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(Department assignedDepartment) { 
        this.assignedDepartment = assignedDepartment; 
        this.updatedAt = LocalDateTime.now();
    }

    public String getDepartmentRemarks() { return departmentRemarks; }
    public void setDepartmentRemarks(String departmentRemarks) { 
        this.departmentRemarks = departmentRemarks; 
        this.updatedAt = LocalDateTime.now();
    }

    // Comments getters and setters
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
}
