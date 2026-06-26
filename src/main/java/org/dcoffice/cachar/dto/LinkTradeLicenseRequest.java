package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class LinkTradeLicenseRequest {
    @NotBlank(message = "License number is required")
    private String licenseNumber;

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
