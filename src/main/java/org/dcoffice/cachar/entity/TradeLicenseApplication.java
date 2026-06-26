package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "trade_license_applications")
public class TradeLicenseApplication {
    @Id
    private String id;

    @Indexed(unique = true)
    private String applicationNumber;
    private String citizenId;
    private String smcCitizenId;
    private String licenseNumber;
    private String applicationType;
    private String applicantName;
    private String mobileNumber;
    private String businessName;
    private String tradeType;
    private String businessAddress;
    private Integer wardNumber;
    private String locality;
    private String remarks;
    private String status = "SUBMITTED";
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private String paymentStatus = "NOT_REQUIRED";
    private String paymentMode;
    private String paymentReference;
    private String receiptNumber;
    private LocalDateTime paidAt;
    private String processedByOfficerId;
    private String processedByOfficerName;
    private String officerRemarks;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private Integer citizenRating;
    private String citizenFeedback;
    private LocalDateTime feedbackAt;
    private String provider = "UPYOG_READY";
    private String upyogApplicationId;
    private String upyogBusinessService = "TL";
    private String upyogPaymentConsumerCode;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getSmcCitizenId() { return smcCitizenId; }
    public void setSmcCitizenId(String smcCitizenId) { this.smcCitizenId = smcCitizenId; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public Integer getWardNumber() { return wardNumber; }
    public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getProcessedByOfficerId() { return processedByOfficerId; }
    public void setProcessedByOfficerId(String processedByOfficerId) { this.processedByOfficerId = processedByOfficerId; }
    public String getProcessedByOfficerName() { return processedByOfficerName; }
    public void setProcessedByOfficerName(String processedByOfficerName) { this.processedByOfficerName = processedByOfficerName; }
    public String getOfficerRemarks() { return officerRemarks; }
    public void setOfficerRemarks(String officerRemarks) { this.officerRemarks = officerRemarks; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public Integer getCitizenRating() { return citizenRating; }
    public void setCitizenRating(Integer citizenRating) { this.citizenRating = citizenRating; }
    public String getCitizenFeedback() { return citizenFeedback; }
    public void setCitizenFeedback(String citizenFeedback) { this.citizenFeedback = citizenFeedback; }
    public LocalDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(LocalDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getUpyogApplicationId() { return upyogApplicationId; }
    public void setUpyogApplicationId(String upyogApplicationId) { this.upyogApplicationId = upyogApplicationId; }
    public String getUpyogBusinessService() { return upyogBusinessService; }
    public void setUpyogBusinessService(String upyogBusinessService) { this.upyogBusinessService = upyogBusinessService; }
    public String getUpyogPaymentConsumerCode() { return upyogPaymentConsumerCode; }
    public void setUpyogPaymentConsumerCode(String upyogPaymentConsumerCode) { this.upyogPaymentConsumerCode = upyogPaymentConsumerCode; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
