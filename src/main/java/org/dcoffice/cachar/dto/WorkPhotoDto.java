package org.dcoffice.cachar.dto;

public class WorkPhotoDto {
    private String id;
    private String userId;
    private String memberName;
    private String imageUri;
    private String notes;
    private LocationCoordsDto location;
    private String uploadedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocationCoordsDto getLocation() { return location; }
    public void setLocation(LocationCoordsDto location) { this.location = location; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
