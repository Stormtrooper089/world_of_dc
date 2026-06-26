package org.dcoffice.cachar.dto;

import java.math.BigDecimal;

public class AuctionBidRequest {
    private String bidderType;
    private String businessName;
    private String gstNumber;
    private String tradeLicenseNumber;
    private BigDecimal bidAmount;
    private Boolean termsAccepted;

    public String getBidderType() { return bidderType; }
    public void setBidderType(String bidderType) { this.bidderType = bidderType; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
    public String getTradeLicenseNumber() { return tradeLicenseNumber; }
    public void setTradeLicenseNumber(String tradeLicenseNumber) { this.tradeLicenseNumber = tradeLicenseNumber; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }
}
