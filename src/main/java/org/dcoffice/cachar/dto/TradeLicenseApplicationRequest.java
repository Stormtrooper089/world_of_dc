package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class TradeLicenseApplicationRequest {
    private String licenseNumber;

    @NotBlank(message = "Application type is required")
    private String applicationType;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Trade type is required")
    private String tradeType;

    @NotBlank(message = "Business address is required")
    private String businessAddress;

    private Integer wardNumber;
    private String locality;
    private String remarks;

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }
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
}
