package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class LinkPropertyRequest {
    @NotBlank(message = "Holding number is required")
    private String holdingNumber;

    public String getHoldingNumber() { return holdingNumber; }
    public void setHoldingNumber(String holdingNumber) { this.holdingNumber = holdingNumber; }
}
