package org.dcoffice.cachar.dto;

/** audioBase64 is a WAV clip — playable directly as a data: URI on the frontend. */
public class TextToSpeechResponse {

    private String audioBase64;
    private String audioFormat;

    public TextToSpeechResponse() {
    }

    public TextToSpeechResponse(String audioBase64, String audioFormat) {
        this.audioBase64 = audioBase64;
        this.audioFormat = audioFormat;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }
}
