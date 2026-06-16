package org.dcoffice.cachar.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WastePickupAuditTrail {
    private String action;
    private WastePickupStatus previousStatus;
    private WastePickupStatus newStatus;
    private String remarks;
    private String actorId;
    private String actorRole;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Double latitude;
    private Double longitude;
    private List<String> photos = new ArrayList<>();

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public WastePickupStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(WastePickupStatus previousStatus) { this.previousStatus = previousStatus; }

    public WastePickupStatus getNewStatus() { return newStatus; }
    public void setNewStatus(WastePickupStatus newStatus) { this.newStatus = newStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
}
