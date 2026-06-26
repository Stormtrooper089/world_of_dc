package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "trade_licenses")
public class TradeLicense {
    @Id
    private String id;

    @Indexed(unique = true)
    private String licenseNumber;
    private String smcCitizenId;
    private String linkedCitizenId;
    private String applicantName;
    private String mobileNumber;
    private String businessName;
    private String tradeType;
    private String businessAddress;
    private Integer wardNumber;
    private String wardName;
    private String locality;
    private LocalDate validFrom;
    private LocalDate validTo;
    private BigDecimal annualFee = BigDecimal.ZERO;
    private BigDecimal arrears = BigDecimal.ZERO;
    private BigDecimal penalty = BigDecimal.ZERO;
    private BigDecimal amountDue = BigDecimal.ZERO;
    private String status = "ACTIVE";
    private LocalDateTime linkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TradeLicense() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getSmcCitizenId() { return smcCitizenId; }
    public void setSmcCitizenId(String smcCitizenId) { this.smcCitizenId = smcCitizenId; }
    public String getLinkedCitizenId() { return linkedCitizenId; }
    public void setLinkedCitizenId(String linkedCitizenId) { this.linkedCitizenId = linkedCitizenId; }
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
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public BigDecimal getAnnualFee() { return annualFee; }
    public void setAnnualFee(BigDecimal annualFee) { this.annualFee = annualFee; }
    public BigDecimal getArrears() { return arrears; }
    public void setArrears(BigDecimal arrears) { this.arrears = arrears; }
    public BigDecimal getPenalty() { return penalty; }
    public void setPenalty(BigDecimal penalty) { this.penalty = penalty; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
