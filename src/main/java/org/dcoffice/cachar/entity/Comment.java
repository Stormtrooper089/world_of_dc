package org.dcoffice.cachar.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @NotNull(message = "Complaint ID is required")
    @Indexed
    private String complaintId;

    @NotNull(message = "Commenter ID is required")
    @Indexed
    private String commenterId; // User ID of who made the comment

    private String commenterName; // Name of the commenter for display
    private String commenterRole; // Role of the commenter (OFFICER, CITIZEN, etc.)

    @NotBlank(message = "Comment text is required")
    private String text;

    private List<CommentAttachment> attachments;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Comment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getCommenterId() { return commenterId; }
    public void setCommenterId(String commenterId) { this.commenterId = commenterId; }

    public String getCommenterName() { return commenterName; }
    public void setCommenterName(String commenterName) { this.commenterName = commenterName; }

    public String getCommenterRole() { return commenterRole; }
    public void setCommenterRole(String commenterRole) { this.commenterRole = commenterRole; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<CommentAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<CommentAttachment> attachments) { this.attachments = attachments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
