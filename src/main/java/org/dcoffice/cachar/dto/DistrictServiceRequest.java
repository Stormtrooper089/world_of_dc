package org.dcoffice.cachar.dto;

import java.util.List;

public class DistrictServiceRequest {
    private String serviceName;
    private String department;
    private String category;
    private String description;
    private List<String> eligibility;
    private List<String> requiredDocuments;
    private String feeDescription;
    private String slaTimeline;
    private String serviceMode;
    private String applyUrl;
    private String trackingUrl;
    private String escalationOfficer;
    private String escalationContact;
    private String integrationStatus;
    private Boolean active;
    private Integer displayOrder;

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getEligibility() { return eligibility; }
    public void setEligibility(List<String> eligibility) { this.eligibility = eligibility; }
    public List<String> getRequiredDocuments() { return requiredDocuments; }
    public void setRequiredDocuments(List<String> requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    public String getFeeDescription() { return feeDescription; }
    public void setFeeDescription(String feeDescription) { this.feeDescription = feeDescription; }
    public String getSlaTimeline() { return slaTimeline; }
    public void setSlaTimeline(String slaTimeline) { this.slaTimeline = slaTimeline; }
    public String getServiceMode() { return serviceMode; }
    public void setServiceMode(String serviceMode) { this.serviceMode = serviceMode; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
    public String getEscalationOfficer() { return escalationOfficer; }
    public void setEscalationOfficer(String escalationOfficer) { this.escalationOfficer = escalationOfficer; }
    public String getEscalationContact() { return escalationContact; }
    public void setEscalationContact(String escalationContact) { this.escalationContact = escalationContact; }
    public String getIntegrationStatus() { return integrationStatus; }
    public void setIntegrationStatus(String integrationStatus) { this.integrationStatus = integrationStatus; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
