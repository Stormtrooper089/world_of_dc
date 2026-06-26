package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "auction_bids")
public class AuctionBid {
    @Id
    private String id;

    @Indexed(unique = true)
    private String bidId;
    @Indexed
    private String auctionId;
    private String bidderCitizenId;
    private String smcCitizenId;
    private String bidderName;
    private String bidderMobile;
    private String bidderEmail;
    private String bidderType;
    private String businessName;
    private String gstNumber;
    private String tradeLicenseNumber;
    private BigDecimal bidAmount = BigDecimal.ZERO;
    private String bidStatus = "ACTIVE";
    private Boolean termsAccepted = false;
    private String emdPaymentStatus = "PENDING";
    private LocalDateTime submittedAt;
    private String rejectedReason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBidId() { return bidId; }
    public void setBidId(String bidId) { this.bidId = bidId; }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getBidderCitizenId() { return bidderCitizenId; }
    public void setBidderCitizenId(String bidderCitizenId) { this.bidderCitizenId = bidderCitizenId; }
    public String getSmcCitizenId() { return smcCitizenId; }
    public void setSmcCitizenId(String smcCitizenId) { this.smcCitizenId = smcCitizenId; }
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public String getBidderMobile() { return bidderMobile; }
    public void setBidderMobile(String bidderMobile) { this.bidderMobile = bidderMobile; }
    public String getBidderEmail() { return bidderEmail; }
    public void setBidderEmail(String bidderEmail) { this.bidderEmail = bidderEmail; }
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
    public String getBidStatus() { return bidStatus; }
    public void setBidStatus(String bidStatus) { this.bidStatus = bidStatus; }
    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }
    public String getEmdPaymentStatus() { return emdPaymentStatus; }
    public void setEmdPaymentStatus(String emdPaymentStatus) { this.emdPaymentStatus = emdPaymentStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
}
