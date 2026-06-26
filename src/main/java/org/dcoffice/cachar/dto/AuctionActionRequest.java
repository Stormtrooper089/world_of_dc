package org.dcoffice.cachar.dto;

public class AuctionActionRequest {
    private String remarks;
    private String reason;
    private String bidId;

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBidId() { return bidId; }
    public void setBidId(String bidId) { this.bidId = bidId; }
}
