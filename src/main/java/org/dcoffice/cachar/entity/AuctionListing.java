package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "auction_listings")
public class AuctionListing {
    @Id
    private String id;

    @Indexed(unique = true)
    private String auctionId;
    private String title;
    private String description;
    private String category;
    private String resourceType;
    private String department;
    private Integer wardNumber;
    private String wardName;
    private String zone;
    private String locality;
    private String address;
    private Double latitude;
    private Double longitude;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private BigDecimal reservePrice = BigDecimal.ZERO;
    private BigDecimal bidIncrement = BigDecimal.ZERO;
    private BigDecimal emdAmount = BigDecimal.ZERO;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime inspectionAt;
    private String status = "DRAFT";
    private String eligibilityCriteria;
    private String termsAndConditions;
    private List<AuctionDocument> documents = new ArrayList<>();
    private List<AuctionDocument> photos = new ArrayList<>();
    private String createdByOfficerId;
    private String createdByOfficerName;
    private String awardedBidId;
    private String awardedBidderId;
    private LocalDateTime awardedAt;
    private String cancellationReason;
    private BigDecimal currentHighestBid = BigDecimal.ZERO;
    private String currentHighestBidId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AuctionListing() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getWardNumber() { return wardNumber; }
    public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }
    public BigDecimal getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(BigDecimal bidIncrement) { this.bidIncrement = bidIncrement; }
    public BigDecimal getEmdAmount() { return emdAmount; }
    public void setEmdAmount(BigDecimal emdAmount) { this.emdAmount = emdAmount; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public LocalDateTime getInspectionAt() { return inspectionAt; }
    public void setInspectionAt(LocalDateTime inspectionAt) { this.inspectionAt = inspectionAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEligibilityCriteria() { return eligibilityCriteria; }
    public void setEligibilityCriteria(String eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }
    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }
    public List<AuctionDocument> getDocuments() { return documents; }
    public void setDocuments(List<AuctionDocument> documents) { this.documents = documents; }
    public List<AuctionDocument> getPhotos() { return photos; }
    public void setPhotos(List<AuctionDocument> photos) { this.photos = photos; }
    public String getCreatedByOfficerId() { return createdByOfficerId; }
    public void setCreatedByOfficerId(String createdByOfficerId) { this.createdByOfficerId = createdByOfficerId; }
    public String getCreatedByOfficerName() { return createdByOfficerName; }
    public void setCreatedByOfficerName(String createdByOfficerName) { this.createdByOfficerName = createdByOfficerName; }
    public String getAwardedBidId() { return awardedBidId; }
    public void setAwardedBidId(String awardedBidId) { this.awardedBidId = awardedBidId; }
    public String getAwardedBidderId() { return awardedBidderId; }
    public void setAwardedBidderId(String awardedBidderId) { this.awardedBidderId = awardedBidderId; }
    public LocalDateTime getAwardedAt() { return awardedAt; }
    public void setAwardedAt(LocalDateTime awardedAt) { this.awardedAt = awardedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public String getCurrentHighestBidId() { return currentHighestBidId; }
    public void setCurrentHighestBidId(String currentHighestBidId) { this.currentHighestBidId = currentHighestBidId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
