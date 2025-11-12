package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "meetings")
public class Meeting {

    @Id
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start date and time is required")
    @Indexed
    private LocalDateTime startDateTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes; // Duration in minutes

    @NotBlank(message = "Created by is required")
    @Indexed
    private String createdById; // Officer ID who created the meeting

    @NotNull(message = "At least one officer must be invited")
    private List<String> invitedOfficerIds; // List of officer IDs invited to the meeting

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Meeting() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public List<String> getInvitedOfficerIds() {
        return invitedOfficerIds;
    }

    public void setInvitedOfficerIds(List<String> invitedOfficerIds) {
        this.invitedOfficerIds = invitedOfficerIds;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

