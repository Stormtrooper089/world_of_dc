package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "waste_pickup_requests")
public class WastePickupRequest {
    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingId;

    private String citizenName;
    @Indexed
    private String citizenMobile;
    @Indexed
    private Integer wardNumber;
    private String locality;
    private String landmark;
    private String fullAddress;
    private Double latitude;
    private Double longitude;

    @Indexed
    private WasteCategory wasteCategory = WasteCategory.HOUSEHOLD_WASTE_NOT_COLLECTED;
    private WasteQuantityEstimate estimatedQuantity = WasteQuantityEstimate.SMALL;
    @Indexed
    private WasteUrgency urgency = WasteUrgency.NORMAL;
    private String description;
    private List<String> beforePhotos = new ArrayList<>();
    private String preferredPickupSlot;
    private Boolean vehicleAccessAvailable;
    private String sensitiveNearbyLocation;

    @Indexed
    private WastePickupStatus status = WastePickupStatus.SUBMITTED;
    private String assignedOfficerId;
    private String assignedSanitationStaffId;
    private String assignedVehicleNumber;
    private String sanitationTeamName;

    @Indexed
    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime verifiedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime closedAt;

    private WasteQuantityEstimate actualWasteQuantity;
    private List<String> afterPhotos = new ArrayList<>();
    private Double closureLatitude;
    private Double closureLongitude;
    private String closureRemarks;
    private Boolean siteFullyCleaned;
    private Integer citizenRating;
    private String citizenFeedback;
    private String reopenedReason;
    private Integer escalationLevel = 0;
    private Integer slaHours = 48;
    @Indexed
    private Boolean slaBreached = false;
    private List<WastePickupAuditTrail> auditTrail = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenMobile() { return citizenMobile; }
    public void setCitizenMobile(String citizenMobile) { this.citizenMobile = citizenMobile; }

    public Integer getWardNumber() { return wardNumber; }
    public void setWardNumber(Integer wardNumber) { this.wardNumber = wardNumber; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public WasteCategory getWasteCategory() { return wasteCategory; }
    public void setWasteCategory(WasteCategory wasteCategory) { this.wasteCategory = wasteCategory; }

    public WasteQuantityEstimate getEstimatedQuantity() { return estimatedQuantity; }
    public void setEstimatedQuantity(WasteQuantityEstimate estimatedQuantity) { this.estimatedQuantity = estimatedQuantity; }

    public WasteUrgency getUrgency() { return urgency; }
    public void setUrgency(WasteUrgency urgency) {
        this.urgency = urgency;
        this.slaHours = urgency != null ? urgency.getSlaHours() : 48;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getBeforePhotos() { return beforePhotos; }
    public void setBeforePhotos(List<String> beforePhotos) { this.beforePhotos = beforePhotos; }

    public String getPreferredPickupSlot() { return preferredPickupSlot; }
    public void setPreferredPickupSlot(String preferredPickupSlot) { this.preferredPickupSlot = preferredPickupSlot; }

    public Boolean getVehicleAccessAvailable() { return vehicleAccessAvailable; }
    public void setVehicleAccessAvailable(Boolean vehicleAccessAvailable) { this.vehicleAccessAvailable = vehicleAccessAvailable; }

    public String getSensitiveNearbyLocation() { return sensitiveNearbyLocation; }
    public void setSensitiveNearbyLocation(String sensitiveNearbyLocation) { this.sensitiveNearbyLocation = sensitiveNearbyLocation; }

    public WastePickupStatus getStatus() { return status; }
    public void setStatus(WastePickupStatus status) { this.status = status; }

    public String getAssignedOfficerId() { return assignedOfficerId; }
    public void setAssignedOfficerId(String assignedOfficerId) { this.assignedOfficerId = assignedOfficerId; }

    public String getAssignedSanitationStaffId() { return assignedSanitationStaffId; }
    public void setAssignedSanitationStaffId(String assignedSanitationStaffId) { this.assignedSanitationStaffId = assignedSanitationStaffId; }

    public String getAssignedVehicleNumber() { return assignedVehicleNumber; }
    public void setAssignedVehicleNumber(String assignedVehicleNumber) { this.assignedVehicleNumber = assignedVehicleNumber; }

    public String getSanitationTeamName() { return sanitationTeamName; }
    public void setSanitationTeamName(String sanitationTeamName) { this.sanitationTeamName = sanitationTeamName; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public WasteQuantityEstimate getActualWasteQuantity() { return actualWasteQuantity; }
    public void setActualWasteQuantity(WasteQuantityEstimate actualWasteQuantity) { this.actualWasteQuantity = actualWasteQuantity; }

    public List<String> getAfterPhotos() { return afterPhotos; }
    public void setAfterPhotos(List<String> afterPhotos) { this.afterPhotos = afterPhotos; }

    public Double getClosureLatitude() { return closureLatitude; }
    public void setClosureLatitude(Double closureLatitude) { this.closureLatitude = closureLatitude; }

    public Double getClosureLongitude() { return closureLongitude; }
    public void setClosureLongitude(Double closureLongitude) { this.closureLongitude = closureLongitude; }

    public String getClosureRemarks() { return closureRemarks; }
    public void setClosureRemarks(String closureRemarks) { this.closureRemarks = closureRemarks; }

    public Boolean getSiteFullyCleaned() { return siteFullyCleaned; }
    public void setSiteFullyCleaned(Boolean siteFullyCleaned) { this.siteFullyCleaned = siteFullyCleaned; }

    public Integer getCitizenRating() { return citizenRating; }
    public void setCitizenRating(Integer citizenRating) { this.citizenRating = citizenRating; }

    public String getCitizenFeedback() { return citizenFeedback; }
    public void setCitizenFeedback(String citizenFeedback) { this.citizenFeedback = citizenFeedback; }

    public String getReopenedReason() { return reopenedReason; }
    public void setReopenedReason(String reopenedReason) { this.reopenedReason = reopenedReason; }

    public Integer getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public Boolean getSlaBreached() { return slaBreached; }
    public void setSlaBreached(Boolean slaBreached) { this.slaBreached = slaBreached; }

    public List<WastePickupAuditTrail> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<WastePickupAuditTrail> auditTrail) { this.auditTrail = auditTrail; }
}
