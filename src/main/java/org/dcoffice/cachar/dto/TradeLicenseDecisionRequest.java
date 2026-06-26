package org.dcoffice.cachar.dto;

import java.math.BigDecimal;

public class TradeLicenseDecisionRequest {
    private BigDecimal payableAmount;
    private String remarks;
    private String rejectionReason;

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
