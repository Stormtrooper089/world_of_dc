package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

/**
 * Inbound message for the citizen voice assistant.
 *
 * The endpoint is intentionally public/anonymous (no JWT), matching the existing
 * pattern used by /api/complaints/track/** and /api/citizen/register — a citizen
 * calling in by voice has not logged in. mobileNumber is optional and only gets
 * set once the citizen has stated/confirmed it during the conversation (needed
 * before we allow the "create_complaint" tool to run — see VoiceAssistantService).
 */
public class VoiceChatRequest {

    @NotBlank(message = "Session id is required")
    private String sessionId;

    @NotBlank(message = "Transcript is required")
    private String transcript;

    private String mobileNumber;

    public VoiceChatRequest() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}
