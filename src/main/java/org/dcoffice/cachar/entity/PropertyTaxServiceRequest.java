package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "property_tax_service_requests")
public class PropertyTaxServiceRequest {
    @Id
    private String id;
    @Indexed(unique = true)
    private String requestNumber;
    @Indexed
    private String citizenId;
    private String smcCitizenId;
    @Indexed
    private String holdingNumber;
    private String requestType;
    private String applicantName;
    private String mobileNumber;
    private String remarks;
    private String status = "SUBMITTED";
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getSmcCitizenId() { return smcCitizenId; }
    public void setSmcCitizenId(String smcCitizenId) { this.smcCitizenId = smcCitizenId; }
    public String getHoldingNumber() { return holdingNumber; }
    public void setHoldingNumber(String holdingNumber) { this.holdingNumber = holdingNumber; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
