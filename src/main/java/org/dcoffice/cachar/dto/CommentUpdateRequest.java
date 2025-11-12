package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CommentUpdateRequest {

    @NotNull(message = "Comment ID is required")
    private String commentId;

    @NotBlank(message = "Comment text is required")
    private String text;

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
