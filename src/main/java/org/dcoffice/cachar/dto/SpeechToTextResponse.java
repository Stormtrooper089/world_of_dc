package org.dcoffice.cachar.dto;

public class SpeechToTextResponse {

    private String transcript;

    public SpeechToTextResponse() {
    }

    public SpeechToTextResponse(String transcript) {
        this.transcript = transcript;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
}
