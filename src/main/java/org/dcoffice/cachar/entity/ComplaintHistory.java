package org.dcoffice.cachar.entity;
import java.time.LocalDateTime;

public class ComplaintHistory {

    private String id;
    private String officerId;
    private String officerName;
    private String officerDesignation;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private String remarks;
    private LocalDateTime timestamp;
    private Long complaintId;
    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }
    public ComplaintHistory() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOfficerId() { return officerId; }
    public void setOfficerId(String officerId) { this.officerId = officerId; }

    public String getOfficerName() { return officerName; }
    public void setOfficerName(String officerName) { this.officerName = officerName; }

    public String getOfficerDesignation() { return officerDesignation; }
    public void setOfficerDesignation(String officerDesignation) { this.officerDesignation = officerDesignation; }

    public ComplaintStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ComplaintStatus previousStatus) { this.previousStatus = previousStatus; }

    public ComplaintStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ComplaintStatus newStatus) { this.newStatus = newStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
