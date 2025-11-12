package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class MeetingCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start date and time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @NotNull(message = "At least one officer must be invited")
    private List<String> invitedOfficerIds;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<String> getInvitedOfficerIds() {
        return invitedOfficerIds;
    }

    public void setInvitedOfficerIds(List<String> invitedOfficerIds) {
        this.invitedOfficerIds = invitedOfficerIds;
    }
}

