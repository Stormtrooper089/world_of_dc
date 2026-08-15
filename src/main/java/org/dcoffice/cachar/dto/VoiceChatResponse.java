package org.dcoffice.cachar.dto;

/**
 * Reply from the voice assistant. replyText is what the frontend hands to the
 * browser's speech synthesizer (or a cloud TTS call later — see the plan doc).
 */
public class VoiceChatResponse {

    private String sessionId;
    private String replyText;

    // Set when a tool actually mutated/read backend state, so the frontend can
    // show a confirmation chip in the transcript instead of just plain text.
    // One of: "COMPLAINT_CREATED", "COMPLAINT_STATUS", "SERVICE_INFO", or null.
    private String actionTaken;
    private String complaintNumber;

    public VoiceChatResponse() {
    }

    public VoiceChatResponse(String sessionId, String replyText, String actionTaken, String complaintNumber) {
        this.sessionId = sessionId;
        this.replyText = replyText;
        this.actionTaken = actionTaken;
        this.complaintNumber = complaintNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }
}
