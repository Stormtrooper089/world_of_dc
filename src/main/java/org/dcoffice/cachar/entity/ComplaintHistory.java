package org.dcoffice.cachar.entity;

import java.time.LocalDateTime;

public class ComplaintHistory {

    private String id;
    private Long complaintId;
    private String complaintNumber;

    private String officerId;         // Can be null for citizen actions
    private String actorName;         // "Citizen" or officer name
    private String officerDesignation;

    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private String remarks;
    private LocalDateTime timestamp;

    public ComplaintHistory() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public String getOfficerId() { return officerId; }
    public void setOfficerId(String officerId) { this.officerId = officerId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

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