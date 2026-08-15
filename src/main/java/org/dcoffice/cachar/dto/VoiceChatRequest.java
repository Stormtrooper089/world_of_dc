package org.dcoffice.cachar.dto;

import javax.validation.constraints.NotBlank;

/**
 * Inbound message for the citizen voice assistant.
 *
 * There is deliberately no mobileNumber (or any other identity) field here
 * anymore — trusting an identity supplied by the client body would let anyone
 * file a complaint as any citizen just by naming their mobile number. Identity
 * instead comes from the request's JWT, the same way every other citizen
 * endpoint in this codebase gets it: VoiceAssistantController reads the
 * Authentication Spring Security already populated (see JwtAuthenticationFilter
 * — it runs on this path since it isn't in that filter's shouldNotFilter list)
 * and passes the verified citizenId/mobileNumber down to VoiceAssistantService.
 * A citizen who hasn't logged in can still talk to the assistant (general
 * Q&A, status lookup by complaint number) — they just won't get the
 * "I already know your number" experience for filing a new complaint.
 */
public class VoiceChatRequest {

    @NotBlank(message = "Session id is required")
    private String sessionId;

    @NotBlank(message = "Transcript is required")
    private String transcript;

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
}
