package org.dcoffice.cachar.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionListingRequest {
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
    private BigDecimal basePrice;
    private BigDecimal reservePrice;
    private BigDecimal bidIncrement;
    private BigDecimal emdAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime inspectionAt;
    private String eligibilityCriteria;
    private String termsAndConditions;

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
    public String getEligibilityCriteria() { return eligibilityCriteria; }
    public void setEligibilityCriteria(String eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }
    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }
}
