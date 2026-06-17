package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class PropertyTaxServiceRequestDto {
    @NotBlank(message = "Holding number is required")
    private String holdingNumber;
    @NotBlank(message = "Request type is required")
    private String requestType;
    private String remarks;

    public String getHoldingNumber() { return holdingNumber; }
    public void setHoldingNumber(String holdingNumber) { this.holdingNumber = holdingNumber; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
