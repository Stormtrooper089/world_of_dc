package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CommentCreateRequest {

    @NotNull(message = "Complaint ID is required")
    private String complaintId;

    @NotBlank(message = "Comment text is required")
    private String text;

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
