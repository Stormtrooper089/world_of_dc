package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

public class PayPropertyTaxRequest {
    @NotBlank(message = "Holding number is required")
    private String holdingNumber;
    private String paymentMode = "ONLINE_SIMULATION";

    public String getHoldingNumber() { return holdingNumber; }
    public void setHoldingNumber(String holdingNumber) { this.holdingNumber = holdingNumber; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
}
