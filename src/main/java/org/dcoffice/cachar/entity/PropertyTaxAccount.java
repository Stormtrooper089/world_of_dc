package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "property_tax_accounts")
public class PropertyTaxAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String holdingNumber;
    private String assessmentNumber;
    private String ownerName;
    private String mobileNumber;
    private Integer wardNumber;
    private String wardName;
    private String locality;
    private String address;
    private String propertyType;
    private String usageType;
    private String financialYear;
    private BigDecimal annualTax = BigDecimal.ZERO;
    private BigDecimal arrears = BigDecimal.ZERO;
    private BigDecimal penalty = BigDecimal.ZERO;
    private BigDecimal rebate = BigDecimal.ZERO;
    private BigDecimal amountDue = BigDecimal.ZERO;
    private LocalDate dueDate;
    private String status = "DUE";
    @Indexed
    private String linkedCitizenId;
    private LocalDateTime linkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PropertyTaxAccount() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHoldingNumber() { return holdingNumber; }
    public void setHoldingNumber(String holdingNumber) { this.holdingNumber = holdingNumber; }
    public String getAssessmentNumber() { return assessmentNumber; }
    public void setAssessmentNumber(String assessmentNumber) { this.assessmentNumber = assessmentNumber; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public Integer getWardNumber() { return wardNumber; }
    public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getFinancialYear() { return financialYear; }
    public void setFinancialYear(String financialYear) { this.financialYear = financialYear; }
    public BigDecimal getAnnualTax() { return annualTax; }
    public void setAnnualTax(BigDecimal annualTax) { this.annualTax = annualTax; }
    public BigDecimal getArrears() { return arrears; }
    public void setArrears(BigDecimal arrears) { this.arrears = arrears; }
    public BigDecimal getPenalty() { return penalty; }
    public void setPenalty(BigDecimal penalty) { this.penalty = penalty; }
    public BigDecimal getRebate() { return rebate; }
    public void setRebate(BigDecimal rebate) { this.rebate = rebate; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLinkedCitizenId() { return linkedCitizenId; }
    public void setLinkedCitizenId(String linkedCitizenId) { this.linkedCitizenId = linkedCitizenId; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
